package digital.vasic.yole.network.auth

import digital.vasic.yole.network.platform.SecureStorage
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.Duration
import kotlin.test.*

/**
 * Comprehensive test suite for AuthTokenManager
 * Tests token storage, retrieval, expiration checking, and refresh scenarios
 */
class AuthTokenManagerTest {
    
    private lateinit var secureStorage: SecureStorage
    private lateinit var authTokenManager: AuthTokenManager
    private val testServiceName = "test-service"
    
    @BeforeTest
    fun setup() = runTest {
        secureStorage = MockSecureStorage()
        authTokenManager = AuthTokenManager(testServiceName, secureStorage)
    }
    
    @Test
    fun testStoreAndRetrieveAccessToken() = runTest {
        val testToken = "test-access-token-12345"
        
        // Store access token
        val storeResult = authTokenManager.storeAccessToken(testToken)
        assertTrue(storeResult.isSuccess, "Storing access token should succeed")
        
        // Retrieve access token
        val retrieveResult = authTokenManager.getAccessToken()
        assertTrue(retrieveResult.isSuccess, "Retrieving access token should succeed")
        assertEquals(testToken, retrieveResult.getOrNull(), "Retrieved token should match stored token")
    }
    
    @Test
    fun testStoreAndRetrieveRefreshToken() = runTest {
        val testRefreshToken = "test-refresh-token-67890"
        
        // Store refresh token
        val storeResult = authTokenManager.storeRefreshToken(testRefreshToken)
        assertTrue(storeResult.isSuccess, "Storing refresh token should succeed")
        
        // Retrieve refresh token
        val retrieveResult = authTokenManager.getRefreshToken()
        assertTrue(retrieveResult.isSuccess, "Retrieving refresh token should succeed")
        assertEquals(testRefreshToken, retrieveResult.getOrNull(), "Retrieved refresh token should match stored token")
    }
    
    @Test
    fun testStoreAndCheckTokenExpiration() = runTest {
        val futureTime = Clock.System.now().plus(Duration.hours(1))
        
        // Store expiration time
        val storeResult = authTokenManager.storeTokenExpiration(futureTime)
        assertTrue(storeResult.isSuccess, "Storing token expiration should succeed")
        
        // Check if token is expired (should not be)
        val isExpiredResult = authTokenManager.isTokenExpired()
        assertTrue(isExpiredResult.isSuccess, "Checking token expiration should succeed")
        assertFalse(isExpiredResult.getOrNull() ?: true, "Token should not be expired")
    }
    
    @Test
    fun testExpiredTokenDetection() = runTest {
        val pastTime = Clock.System.now().minus(Duration.hours(1))
        
        // Store past expiration time
        val storeResult = authTokenManager.storeTokenExpiration(pastTime)
        assertTrue(storeResult.isSuccess, "Storing past expiration time should succeed")
        
        // Check if token is expired (should be)
        val isExpiredResult = authTokenManager.isTokenExpired()
        assertTrue(isExpiredResult.isSuccess, "Checking token expiration should succeed")
        assertTrue(isExpiredResult.getOrNull() ?: false, "Token should be expired")
    }
    
    @Test
    fun testHasValidToken() = runTest {
        // Initially should not have valid token
        val initialCheck = authTokenManager.hasValidToken()
        assertTrue(initialCheck.isSuccess, "Initial token check should succeed")
        assertFalse(initialCheck.getOrNull() ?: true, "Should not have valid token initially")
        
        // Store valid token with future expiration
        val futureTime = Clock.System.now().plus(Duration.hours(2))
        authTokenManager.storeAccessToken("valid-token")
        authTokenManager.storeTokenExpiration(futureTime)
        
        // Now should have valid token
        val validCheck = authTokenManager.hasValidToken()
        assertTrue(validCheck.isSuccess, "Token validation should succeed")
        assertTrue(validCheck.getOrNull() ?: false, "Should have valid token")
    }
    
    @Test
    fun testHasValidTokenWithExpiredToken() = runTest {
        // Store token with past expiration
        val pastTime = Clock.System.now().minus(Duration.hours(1))
        authTokenManager.storeAccessToken("expired-token")
        authTokenManager.storeTokenExpiration(pastTime)
        
        // Should not have valid token
        val expiredCheck = authTokenManager.hasValidToken()
        assertTrue(expiredCheck.isSuccess, "Token validation should succeed")
        assertFalse(expiredCheck.getOrNull() ?: true, "Should not have valid expired token")
    }
    
    @Test
    fun testClearTokens() = runTest {
        // Store tokens
        authTokenManager.storeAccessToken("access-token")
        authTokenManager.storeRefreshToken("refresh-token")
        val futureTime = Clock.System.now().plus(Duration.hours(1))
        authTokenManager.storeTokenExpiration(futureTime)
        
        // Verify tokens exist
        assertNotNull(authTokenManager.getAccessToken().getOrNull())
        assertNotNull(authTokenManager.getRefreshToken().getOrNull())
        
        // Clear tokens
        val clearResult = authTokenManager.clearTokens()
        assertTrue(clearResult.isSuccess, "Clearing tokens should succeed")
        
        // Verify tokens are cleared
        val accessTokenAfterClear = authTokenManager.getAccessToken().getOrNull()
        val refreshTokenAfterClear = authTokenManager.getRefreshToken().getOrNull()
        
        assertNull(accessTokenAfterClear, "Access token should be null after clearing")
        assertNull(refreshTokenAfterClear, "Refresh token should be null after clearing")
    }
    
    @Test
    fun testStoreTokenInfo() = runTest {
        val accessToken = "test-access-token"
        val refreshToken = "test-refresh-token"
        val expiresIn = 3600L // 1 hour
        
        // Store complete token info
        val storeResult = authTokenManager.storeTokenInfo(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = expiresIn
        )
        assertTrue(storeResult.isSuccess, "Storing complete token info should succeed")
        
        // Verify all tokens are stored
        val retrievedAccessToken = authTokenManager.getAccessToken().getOrNull()
        val retrievedRefreshToken = authTokenManager.getRefreshToken().getOrNull()
        val isExpired = authTokenManager.isTokenExpired().getOrNull()
        
        assertEquals(accessToken, retrievedAccessToken, "Access token should match")
        assertEquals(refreshToken, retrievedRefreshToken, "Refresh token should match")
        assertFalse(isExpired ?: true, "Token should not be expired")
    }
    
    @Test
    fun testStoreTokenInfoWithoutRefreshToken() = runTest {
        val accessToken = "test-access-token"
        val expiresIn = 3600L
        
        // Store token info without refresh token
        val storeResult = authTokenManager.storeTokenInfo(
            accessToken = accessToken,
            refreshToken = null,
            expiresIn = expiresIn
        )
        assertTrue(storeResult.isSuccess, "Storing token info without refresh token should succeed")
        
        // Verify access token is stored but refresh token is not
        val retrievedAccessToken = authTokenManager.getAccessToken().getOrNull()
        val retrievedRefreshToken = authTokenManager.getRefreshToken().getOrNull()
        
        assertEquals(accessToken, retrievedAccessToken, "Access token should match")
        assertNull(retrievedRefreshToken, "Refresh token should be null")
    }
    
    @Test
    fun testStoreTokenInfoWithoutExpiration() = runTest {
        val accessToken = "test-access-token"
        val refreshToken = "test-refresh-token"
        
        // Store token info without expiration
        val storeResult = authTokenManager.storeTokenInfo(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = null
        )
        assertTrue(storeResult.isSuccess, "Storing token info without expiration should succeed")
        
        // Verify tokens are stored but expiration is not set (assumes expired)
        val retrievedAccessToken = authTokenManager.getAccessToken().getOrNull()
        val retrievedRefreshToken = authTokenManager.getRefreshToken().getOrNull()
        val isExpired = authTokenManager.isTokenExpired().getOrNull()
        
        assertEquals(accessToken, retrievedAccessToken, "Access token should match")
        assertEquals(refreshToken, retrievedRefreshToken, "Refresh token should match")
        assertTrue(isExpired ?: false, "Token should be considered expired without expiration time")
    }
    
    @Test
    fun testGetTokenInfo() = runTest {
        // Initially should have no token info
        val initialInfo = authTokenManager.getTokenInfo().getOrNull()
        assertNotNull(initialInfo, "Token info should be available")
        assertFalse(initialInfo!!.hasAccessToken, "Should not have access token initially")
        assertFalse(initialInfo.hasRefreshToken, "Should not have refresh token initially")
        assertTrue(initialInfo.isExpired, "Should be considered expired initially")
        assertEquals(testServiceName, initialInfo.serviceName, "Service name should match")
        
        // Store tokens
        authTokenManager.storeAccessToken("access-token")
        authTokenManager.storeRefreshToken("refresh-token")
        val futureTime = Clock.System.now().plus(Duration.hours(2))
        authTokenManager.storeTokenExpiration(futureTime)
        
        // Check token info with valid tokens
        val validInfo = authTokenManager.getTokenInfo().getOrNull()
        assertNotNull(validInfo, "Token info should be available")
        assertTrue(validInfo!!.hasAccessToken, "Should have access token")
        assertTrue(validInfo.hasRefreshToken, "Should have refresh token")
        assertFalse(validInfo.isExpired, "Should not be expired")
        assertEquals(testServiceName, validInfo.serviceName, "Service name should match")
    }
    
    @Test
    fun testTokenInfoTimestamp() = runTest {
        val beforeTimestamp = Clock.System.now()
        
        // Get token info
        val tokenInfo = authTokenManager.getTokenInfo().getOrNull()
        assertNotNull(tokenInfo, "Token info should be available")
        
        val afterTimestamp = Clock.System.now()
        
        // Verify timestamp is within reasonable range
        assertTrue(tokenInfo!!.timestamp >= beforeTimestamp, "Timestamp should be after or equal to before time")
        assertTrue(tokenInfo.timestamp <= afterTimestamp, "Timestamp should be before or equal to after time")
    }
    
    @Test
    fun testMultipleServiceInstances() = runTest {
        val service1 = AuthTokenManager("service1", secureStorage)
        val service2 = AuthTokenManager("service2", secureStorage)
        
        // Store different tokens for different services
        service1.storeAccessToken("token1")
        service2.storeAccessToken("token2")
        
        // Verify tokens are isolated
        assertEquals("token1", service1.getAccessToken().getOrNull(), "Service 1 should have its own token")
        assertEquals("token2", service2.getAccessToken().getOrNull(), "Service 2 should have its own token")
    }
    
    @Test
    fun testErrorHandling() = runTest {
        // Create manager with failing secure storage
        val failingStorage = FailingSecureStorage()
        val failingManager = AuthTokenManager("failing-service", failingStorage)
        
        // Test that operations fail gracefully
        val storeResult = failingManager.storeAccessToken("token")
        assertTrue(storeResult.isFailure, "Store should fail with failing storage")
        
        val retrieveResult = failingManager.getAccessToken()
        assertTrue(retrieveResult.isFailure, "Retrieve should fail with failing storage")
        
        val isExpiredResult = failingManager.isTokenExpired()
        assertTrue(isExpiredResult.isFailure, "Expiration check should fail with failing storage")
        
        val hasValidResult = failingManager.hasValidToken()
        assertTrue(hasValidResult.isFailure, "Valid token check should fail with failing storage")
    }
    
    // Mock implementation of SecureStorage for testing
    private class MockSecureStorage : SecureStorage {
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
    
    // Failing mock implementation for error testing
    private class FailingSecureStorage : SecureStorage {
        override suspend fun store(key: String, value: String): Result<Unit> {
            return Result.failure(Exception("Storage failure"))
        }
        
        override suspend fun retrieve(key: String): Result<String?> {
            return Result.failure(Exception("Storage failure"))
        }
        
        override suspend fun delete(key: String): Result<Unit> {
            return Result.failure(Exception("Storage failure"))
        }
        
        override suspend fun contains(key: String): Result<Boolean> {
            return Result.failure(Exception("Storage failure"))
        }
        
        override suspend fun listKeys(): Result<List<String>> {
            return Result.failure(Exception("Storage failure"))
        }
        
        override suspend fun clear(): Result<Unit> {
            return Result.failure(Exception("Storage failure"))
        }
        
        override suspend fun isSecure(): Result<Boolean> {
            return Result.failure(Exception("Storage failure"))
        }
    }
}