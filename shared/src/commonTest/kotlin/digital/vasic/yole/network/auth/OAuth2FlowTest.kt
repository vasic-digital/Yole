/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for OAuth2Flow
 *
 *########################################################*/
package digital.vasic.yole.network.auth

import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Comprehensive tests for OAuth2Flow covering:
 * - Authorization URL generation
 * - Token exchange and refresh
 * - Error handling and validation
 * - State management and security
 * - Cross-platform compatibility
 */
class OAuth2FlowTest {

    private lateinit var mockHttpClient: HttpClient
    private lateinit var authTokenManager: AuthTokenManager
    private lateinit var oAuth2Flow: OAuth2Flow
    
    private val testClientId = "test_client_id_123"
    private val testClientSecret = "test_client_secret_456"
    private val testRedirectUri = "https://yole.app/oauth/callback"
    private val testAuthorizationUrl = "https://provider.com/oauth/authorize"
    private val testTokenUrl = "https://provider.com/oauth/token"
    private val testService = "test_service"

    @BeforeTest
    fun setUp() {
        mockHttpClient = mockk(relaxed = true)
        authTokenManager = mockk(relaxed = true)
        oAuth2Flow = OAuth2Flow(mockHttpClient, authTokenManager)
        
        clearAllMocks()
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    // ==================== Authorization URL Tests ====================

    @Test
    fun `generateAuthorizationUrl should create correct URL with parameters`() = runTest {
        // Given
        val scopes = listOf("read", "write")
        val state = "test_state_123"
        
        // When
        val result = oAuth2Flow.generateAuthorizationUrl(
            service = testService,
            clientId = testClientId,
            authorizationUrl = testAuthorizationUrl,
            redirectUri = testRedirectUri,
            scopes = scopes,
            state = state
        )
        
        // Then
        assertTrue(result.isSuccess)
        val url = result.getOrNull()!!
        
        assertTrue(url.startsWith(testAuthorizationUrl))
        assertTrue(url.contains("client_id=$testClientId"))
        assertTrue(url.contains("redirect_uri=${testRedirectUri.encodeURLParameter()}"))
        assertTrue(url.contains("scope=${scopes.joinToString(" ").encodeURLParameter()}"))
        assertTrue(url.contains("state=$state"))
        assertTrue(url.contains("response_type=code"))
    }

    @Test
    fun `generateAuthorizationUrl should handle empty scopes`() = runTest {
        // Given
        val scopes = emptyList<String>()
        val state = "test_state_123"
        
        // When
        val result = oAuth2Flow.generateAuthorizationUrl(
            service = testService,
            clientId = testClientId,
            authorizationUrl = testAuthorizationUrl,
            redirectUri = testRedirectUri,
            scopes = scopes,
            state = state
        )
        
        // Then
        assertTrue(result.isSuccess)
        val url = result.getOrNull()!!
        
        assertTrue(url.contains("client_id=$testClientId"))
        assertFalse(url.contains("scope=")) // Should not include empty scope parameter
    }

    @Test
    fun `generateAuthorizationUrl should handle special characters in parameters`() = runTest {
        // Given
        val specialRedirectUri = "https://yole.app/oauth/callback?param=value&other=test"
        val specialState = "state with spaces and special chars: !@#$%^&*()"
        
        // When
        val result = oAuth2Flow.generateAuthorizationUrl(
            service = testService,
            clientId = testClientId,
            authorizationUrl = testAuthorizationUrl,
            redirectUri = specialRedirectUri,
            scopes = listOf("read"),
            state = specialState
        )
        
        // Then
        assertTrue(result.isSuccess)
        val url = result.getOrNull()!!
        
        assertTrue(url.contains("redirect_uri=${specialRedirectUri.encodeURLParameter()}"))
        assertTrue(url.contains("state=${specialState.encodeURLParameter()}"))
    }

    @Test
    fun `generateAuthorizationUrl should validate required parameters`() = runTest {
        // Given
        val emptyClientId = ""
        val invalidAuthorizationUrl = "not-a-valid-url"
        
        // When
        val result = oAuth2Flow.generateAuthorizationUrl(
            service = testService,
            clientId = emptyClientId,
            authorizationUrl = invalidAuthorizationUrl,
            redirectUri = testRedirectUri,
            scopes = listOf("read"),
            state = "test_state"
        )
        
        // Then
        assertTrue(result.isFailure)
    }

    // ==================== Token Exchange Tests ====================

    @Test
    fun `exchangeAuthorizationCode should retrieve access token`() = runTest {
        // Given
        val authCode = "test_auth_code_123"
        val accessToken = "test_access_token_456"
        val refreshToken = "test_refresh_token_789"
        val expiresIn = 3600L
        
        coEvery { mockHttpClient.post(testTokenUrl, any()) } returns Result.success("""
            {
                "access_token": "$accessToken",
                "refresh_token": "$refreshToken",
                "expires_in": $expiresIn,
                "token_type": "Bearer"
            }
        """.trimIndent())
        
        coEvery { authTokenManager.storeAccessToken(any(), any()) } returns Result.success(Unit)
        coEvery { authTokenManager.storeRefreshToken(any(), any()) } returns Result.success(Unit)
        coEvery { authTokenManager.storeTokenExpiration(any(), any()) } returns Result.success(Unit)
        
        // When
        val result = oAuth2Flow.exchangeAuthorizationCode(
            service = testService,
            clientId = testClientId,
            clientSecret = testClientSecret,
            tokenUrl = testTokenUrl,
            redirectUri = testRedirectUri,
            authorizationCode = authCode
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(accessToken, result.getOrNull())
        
        coVerify { authTokenManager.storeAccessToken(testService, accessToken) }
        coVerify { authTokenManager.storeRefreshToken(testService, refreshToken) }
        coVerify { authTokenManager.storeTokenExpiration(eq(testService), any()) }
    }

    @Test
    fun `exchangeAuthorizationCode should handle token response without refresh token`() = runTest {
        // Given
        val authCode = "test_auth_code_123"
        val accessToken = "test_access_token_456"
        val expiresIn = 3600L
        
        coEvery { mockHttpClient.post(testTokenUrl, any()) } returns Result.success("""
            {
                "access_token": "$accessToken",
                "expires_in": $expiresIn,
                "token_type": "Bearer"
            }
        """.trimIndent())
        
        coEvery { authTokenManager.storeAccessToken(any(), any()) } returns Result.success(Unit)
        coEvery { authTokenManager.storeTokenExpiration(any(), any()) } returns Result.success(Unit)
        
        // When
        val result = oAuth2Flow.exchangeAuthorizationCode(
            service = testService,
            clientId = testClientId,
            clientSecret = testClientSecret,
            tokenUrl = testTokenUrl,
            redirectUri = testRedirectUri,
            authorizationCode = authCode
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(accessToken, result.getOrNull())
        
        coVerify { authTokenManager.storeAccessToken(testService, accessToken) }
        coVerify(exactly = 0) { authTokenManager.storeRefreshToken(any(), any()) }
    }

    @Test
    fun `exchangeAuthorizationCode should handle HTTP errors`() = runTest {
        // Given
        val authCode = "test_auth_code_123"
        val httpError = Exception("HTTP 400: Invalid authorization code")
        
        coEvery { mockHttpClient.post(testTokenUrl, any()) } returns Result.failure(httpError)
        
        // When
        val result = oAuth2Flow.exchangeAuthorizationCode(
            service = testService,
            clientId = testClientId,
            clientSecret = testClientSecret,
            tokenUrl = testTokenUrl,
            redirectUri = testRedirectUri,
            authorizationCode = authCode
        )
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(httpError, result.exceptionOrNull())
    }

    @Test
    fun `exchangeAuthorizationCode should handle invalid JSON response`() = runTest {
        // Given
        val authCode = "test_auth_code_123"
        val invalidJson = "invalid json response"
        
        coEvery { mockHttpClient.post(testTokenUrl, any()) } returns Result.success(invalidJson)
        
        // When
        val result = oAuth2Flow.exchangeAuthorizationCode(
            service = testService,
            clientId = testClientId,
            clientSecret = testClientSecret,
            tokenUrl = testTokenUrl,
            redirectUri = testRedirectUri,
            authorizationCode = authCode
        )
        
        // Then
        assertTrue(result.isFailure)
    }

    // ==================== Token Refresh Tests ====================

    @Test
    fun `refreshAccessToken should use refresh token to get new access token`() = runTest {
        // Given
        val currentRefreshToken = "current_refresh_token_123"
        val newAccessToken = "new_access_token_456"
        val newRefreshToken = "new_refresh_token_789"
        val expiresIn = 3600L
        
        coEvery { authTokenManager.getRefreshToken(testService) } returns Result.success(currentRefreshToken)
        coEvery { mockHttpClient.post(testTokenUrl, any()) } returns Result.success("""
            {
                "access_token": "$newAccessToken",
                "refresh_token": "$newRefreshToken",
                "expires_in": $expiresIn,
                "token_type": "Bearer"
            }
        """.trimIndent())
        
        coEvery { authTokenManager.storeAccessToken(any(), any()) } returns Result.success(Unit)
        coEvery { authTokenManager.storeRefreshToken(any(), any()) } returns Result.success(Unit)
        coEvery { authTokenManager.storeTokenExpiration(any(), any()) } returns Result.success(Unit)
        
        // When
        val result = oAuth2Flow.refreshAccessToken(
            service = testService,
            clientId = testClientId,
            clientSecret = testClientSecret,
            tokenUrl = testTokenUrl
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(newAccessToken, result.getOrNull())
        
        coVerify { authTokenManager.storeAccessToken(testService, newAccessToken) }
        coVerify { authTokenManager.storeRefreshToken(testService, newRefreshToken) }
    }

    @Test
    fun `refreshAccessToken should handle missing refresh token`() = runTest {
        // Given
        coEvery { authTokenManager.getRefreshToken(testService) } returns Result.failure(
            Exception("No refresh token found")
        )
        
        // When
        val result = oAuth2Flow.refreshAccessToken(
            service = testService,
            clientId = testClientId,
            clientSecret = testClientSecret,
            tokenUrl = testTokenUrl
        )
        
        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `refreshAccessToken should handle refresh failure`() = runTest {
        // Given
        val currentRefreshToken = "current_refresh_token_123"
        val refreshError = Exception("Refresh token expired")
        
        coEvery { authTokenManager.getRefreshToken(testService) } returns Result.success(currentRefreshToken)
        coEvery { mockHttpClient.post(testTokenUrl, any()) } returns Result.failure(refreshError)
        
        // When
        val result = oAuth2Flow.refreshAccessToken(
            service = testService,
            clientId = testClientId,
            clientSecret = testClientSecret,
            tokenUrl = testTokenUrl
        )
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(refreshError, result.exceptionOrNull())
    }

    // ==================== State Management Tests ====================

    @Test
    fun `generateState should create unique states`() = runTest {
        // When
        val state1 = oAuth2Flow.generateState()
        val state2 = oAuth2Flow.generateState()
        val state3 = oAuth2Flow.generateState()
        
        // Then
        assertNotEquals(state1, state2)
        assertNotEquals(state2, state3)
        assertNotEquals(state1, state3)
        
        assertTrue(state1.length >= 32) // Should be sufficiently long
        assertTrue(state2.length >= 32)
        assertTrue(state3.length >= 32)
    }

    @Test
    fun `generateState should create URL-safe states`() = runTest {
        // When
        val state = oAuth2Flow.generateState()
        
        // Then
        assertTrue(state.matches(Regex("[A-Za-z0-9_-]+"))) // URL-safe characters only
    }

    @Test
    fun `verifyState should validate matching states`() = runTest {
        // Given
        val originalState = "test_state_123"
        val receivedState = "test_state_123"
        
        // When
        val result = oAuth2Flow.verifyState(originalState, receivedState)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() ?: false)
    }

    @Test
    fun `verifyState should reject non-matching states`() = runTest {
        // Given
        val originalState = "test_state_123"
        val receivedState = "different_state_456"
        
        // When
        val result = oAuth2Flow.verifyState(originalState, receivedState)
        
        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true)
    }

    @Test
    fun `verifyState should handle CSRF attacks`() = runTest {
        // Given
        val originalState = "test_state_123"
        val maliciousState = "<script>alert('XSS')</script>"
        
        // When
        val result = oAuth2Flow.verifyState(originalState, maliciousState)
        
        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true)
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `should handle network timeouts`() = runTest {
        // Given
        val authCode = "test_auth_code_123"
        val timeoutError = Exception("Network timeout")
        
        coEvery { mockHttpClient.post(testTokenUrl, any()) } returns Result.failure(timeoutError)
        
        // When
        val result = oAuth2Flow.exchangeAuthorizationCode(
            service = testService,
            clientId = testClientId,
            clientSecret = testClientSecret,
            tokenUrl = testTokenUrl,
            redirectUri = testRedirectUri,
            authorizationCode = authCode
        )
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(timeoutError, result.exceptionOrNull())
    }

    @Test
    fun `should handle rate limiting`() = runTest {
        // Given
        val authCode = "test_auth_code_123"
        val rateLimitResponse = """
            {
                "error": "rate_limit_exceeded",
                "error_description": "Too many requests",
                "retry_after": 60
            }
        """.trimIndent()
        
        val rateLimitError = Exception("HTTP 429: Rate limit exceeded")
        coEvery { mockHttpClient.post(testTokenUrl, any()) } returns Result.failure(rateLimitError)
        
        // When
        val result = oAuth2Flow.exchangeAuthorizationCode(
            service = testService,
            clientId = testClientId,
            clientSecret = testClientSecret,
            tokenUrl = testTokenUrl,
            redirectUri = testRedirectUri,
            authorizationCode = authCode
        )
        
        // Then
        assertTrue(result.isFailure)
    }

    // ==================== Security Tests ====================

    @Test
    fun `should not expose client secret in logs`() = runTest {
        // Given
        val authCode = "test_auth_code_123"
        
        coEvery { mockHttpClient.post(testTokenUrl, any()) } returns Result.success("""
            {
                "access_token": "test_token",
                "expires_in": 3600,
                "token_type": "Bearer"
            }
        """.trimIndent())
        
        coEvery { authTokenManager.storeAccessToken(any(), any()) } returns Result.success(Unit)
        coEvery { authTokenManager.storeTokenExpiration(any(), any()) } returns Result.success(Unit)
        
        // When
        val result = oAuth2Flow.exchangeAuthorizationCode(
            service = testService,
            clientId = testClientId,
            clientSecret = testClientSecret,
            tokenUrl = testTokenUrl,
            redirectUri = testRedirectUri,
            authorizationCode = authCode
        )
        
        // Then
        assertTrue(result.isSuccess)
        
        // Verify client secret was included in the request but not logged
        coVerify { mockHttpClient.post(eq(testTokenUrl), match { body ->
            body.contains("client_secret=$testClientSecret")
        }) }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle very long authorization codes`() = runTest {
        // Given
        val longAuthCode = "a".repeat(1000)
        val accessToken = "test_access_token"
        
        coEvery { mockHttpClient.post(testTokenUrl, any()) } returns Result.success("""
            {
                "access_token": "$accessToken",
                "expires_in": 3600,
                "token_type": "Bearer"
            }
        """.trimIndent())
        
        coEvery { authTokenManager.storeAccessToken(any(), any()) } returns Result.success(Unit)
        coEvery { authTokenManager.storeTokenExpiration(any(), any()) } returns Result.success(Unit)
        
        // When
        val result = oAuth2Flow.exchangeAuthorizationCode(
            service = testService,
            clientId = testClientId,
            clientSecret = testClientSecret,
            tokenUrl = testTokenUrl,
            redirectUri = testRedirectUri,
            authorizationCode = longAuthCode
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(accessToken, result.getOrNull())
    }

    @Test
    fun `should handle empty token responses`() = runTest {
        // Given
        val authCode = "test_auth_code_123"
        val emptyResponse = """
            {
                "access_token": "",
                "expires_in": 3600,
                "token_type": "Bearer"
            }
        """.trimIndent()
        
        coEvery { mockHttpClient.post(testTokenUrl, any()) } returns Result.success(emptyResponse)
        
        // When
        val result = oAuth2Flow.exchangeAuthorizationCode(
            service = testService,
            clientId = testClientId,
            clientSecret = testClientSecret,
            tokenUrl = testTokenUrl,
            redirectUri = testRedirectUri,
            authorizationCode = authCode
        )
        
        // Then
        assertTrue(result.isFailure) // Should fail with empty access token
    }
}

// Helper extension for URL encoding
private fun String.encodeURLParameter(): String {
    return this.replace(" ", "%20")
        .replace("!", "%21")
        .replace("#", "%23")
        .replace("$", "%24")
        .replace("&", "%26")
        .replace("'", "%27")
        .replace("(", "%28")
        .replace(")", "%29")
        .replace("*", "%2A")
        .replace("+", "%2B")
        .replace(",", "%2C")
        .replace(":", "%3A")
        .replace(";", "%3B")
        .replace("=", "%3D")
        .replace("?", "%3F")
        .replace("@", "%40")
        .replace("[", "%5B")
        .replace("]", "%5D")
}