/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for OAuth2Flow
 *
 *########################################################*/
package digital.vasic.yole.network.auth

import digital.vasic.yole.network.protocol.createHttpClient
import kotlin.test.*

/**
 * Comprehensive tests for OAuth2Flow covering:
 * - Authorization URL generation
 * - Token response validation
 * - Subclass construction (Dropbox, Google Drive, OneDrive)
 * - State management and security
 * - Edge cases
 */
class OAuth2FlowTest {

    private val testClientId = "test_client_id_123"
    private val testClientSecret = "test_client_secret_456"
    private val testRedirectUri = "https://yole.app/oauth/callback"
    private val testAuthorizationUrl = "https://provider.com/oauth/authorize"
    private val testTokenUrl = "https://provider.com/oauth/token"
    private val testScopes = listOf("read", "write")

    private lateinit var oAuth2Flow: OAuth2Flow

    @BeforeTest
    fun setUp() {
        val httpClient = createHttpClient()
        oAuth2Flow = OAuth2Flow(
            httpClient = httpClient,
            clientId = testClientId,
            clientSecret = testClientSecret,
            authorizationUrl = testAuthorizationUrl,
            tokenUrl = testTokenUrl,
            redirectUri = testRedirectUri,
            scopes = testScopes
        )
    }

    // ==================== Authorization URL Tests ====================

    @Test
    fun `getAuthorizationUrl should create correct URL with parameters`() {
        // When
        val url = oAuth2Flow.getAuthorizationUrl()

        // Then
        assertTrue(url.startsWith(testAuthorizationUrl))
        assertTrue(url.contains("client_id=$testClientId"))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("access_type=offline"))
        assertTrue(url.contains("prompt=consent"))
    }

    @Test
    fun `getAuthorizationUrl should include redirect URI`() {
        // When
        val url = oAuth2Flow.getAuthorizationUrl()

        // Then
        assertTrue(url.contains("redirect_uri="), "URL should contain redirect_uri parameter")
    }

    @Test
    fun `getAuthorizationUrl should include scopes`() {
        // When
        val url = oAuth2Flow.getAuthorizationUrl()

        // Then
        assertTrue(url.contains("scope="), "URL should contain scope parameter")
    }

    @Test
    fun `getAuthorizationUrl should include state when provided`() {
        // Given
        val state = "test_state_123"

        // When
        val url = oAuth2Flow.getAuthorizationUrl(state = state)

        // Then
        assertTrue(url.contains("state=$state"), "URL should contain state parameter")
    }

    @Test
    fun `getAuthorizationUrl should not include state when null`() {
        // When
        val url = oAuth2Flow.getAuthorizationUrl(state = null)

        // Then
        assertFalse(url.contains("state="), "URL should not contain state when null")
    }

    @Test
    fun `getAuthorizationUrl should handle empty scopes`() {
        // Given
        val httpClient = createHttpClient()
        val flowWithNoScopes = OAuth2Flow(
            httpClient = httpClient,
            clientId = testClientId,
            clientSecret = testClientSecret,
            authorizationUrl = testAuthorizationUrl,
            tokenUrl = testTokenUrl,
            redirectUri = testRedirectUri,
            scopes = emptyList()
        )

        // When
        val url = flowWithNoScopes.getAuthorizationUrl()

        // Then
        assertFalse(url.contains("scope="), "URL should not include scope when scopes are empty")
    }

    @Test
    fun `getAuthorizationUrl with different states produces different URLs`() {
        // When
        val url1 = oAuth2Flow.getAuthorizationUrl(state = "state1")
        val url2 = oAuth2Flow.getAuthorizationUrl(state = "state2")

        // Then
        assertNotEquals(url1, url2, "Different states should produce different URLs")
    }

    // ==================== Token Response Tests ====================

    @Test
    fun `TokenResponse should have correct default values`() {
        // When
        val response = TokenResponse(
            access_token = "test_access_token"
        )

        // Then
        assertEquals("test_access_token", response.access_token)
        assertEquals("Bearer", response.token_type)
        assertNull(response.expires_in)
        assertNull(response.refresh_token)
        assertNull(response.scope)
    }

    @Test
    fun `TokenResponse should handle all fields`() {
        // When
        val response = TokenResponse(
            access_token = "test_access_token",
            token_type = "Bearer",
            expires_in = 3600L,
            refresh_token = "test_refresh_token",
            scope = "read write"
        )

        // Then
        assertEquals("test_access_token", response.access_token)
        assertEquals("Bearer", response.token_type)
        assertEquals(3600L, response.expires_in)
        assertEquals("test_refresh_token", response.refresh_token)
        assertEquals("read write", response.scope)
    }

    @Test
    fun `TokenResponse hasRefreshToken should return true when refresh token present`() {
        // Given
        val response = TokenResponse(
            access_token = "token",
            refresh_token = "refresh_token"
        )

        // Then
        assertTrue(response.hasRefreshToken())
    }

    @Test
    fun `TokenResponse hasRefreshToken should return false when refresh token null`() {
        // Given
        val response = TokenResponse(
            access_token = "token",
            refresh_token = null
        )

        // Then
        assertFalse(response.hasRefreshToken())
    }

    @Test
    fun `TokenResponse hasRefreshToken should return false when refresh token blank`() {
        // Given
        val response = TokenResponse(
            access_token = "token",
            refresh_token = ""
        )

        // Then
        assertFalse(response.hasRefreshToken())
    }

    @Test
    fun `TokenResponse getExpiresInSeconds should return expires_in when present`() {
        // Given
        val response = TokenResponse(
            access_token = "token",
            expires_in = 7200L
        )

        // Then
        assertEquals(7200L, response.getExpiresInSeconds())
    }

    @Test
    fun `TokenResponse getExpiresInSeconds should default to 3600 when null`() {
        // Given
        val response = TokenResponse(
            access_token = "token",
            expires_in = null
        )

        // Then
        assertEquals(3600L, response.getExpiresInSeconds())
    }

    // ==================== Subclass Construction Tests ====================

    @Test
    fun `DropboxOAuth2Flow should be constructable`() {
        // When
        val httpClient = createHttpClient()
        val dropboxFlow = DropboxOAuth2Flow(
            httpClient = httpClient,
            clientId = "dropbox-client-id",
            clientSecret = "dropbox-client-secret",
            redirectUri = "https://yole.app/dropbox/callback"
        )

        // Then
        val url = dropboxFlow.getAuthorizationUrl()
        assertTrue(url.contains("dropbox.com"), "Dropbox flow should use Dropbox URL")
        assertTrue(url.contains("client_id=dropbox-client-id"))
    }

    @Test
    fun `GoogleDriveOAuth2Flow should be constructable`() {
        // When
        val httpClient = createHttpClient()
        val googleFlow = GoogleDriveOAuth2Flow(
            httpClient = httpClient,
            clientId = "google-client-id",
            clientSecret = "google-client-secret",
            redirectUri = "https://yole.app/google/callback"
        )

        // Then
        val url = googleFlow.getAuthorizationUrl()
        assertTrue(url.contains("accounts.google.com"), "Google flow should use Google URL")
        assertTrue(url.contains("client_id=google-client-id"))
    }

    @Test
    fun `OneDriveOAuth2Flow should be constructable`() {
        // When
        val httpClient = createHttpClient()
        val oneDriveFlow = OneDriveOAuth2Flow(
            httpClient = httpClient,
            clientId = "onedrive-client-id",
            clientSecret = "onedrive-client-secret",
            redirectUri = "https://yole.app/onedrive/callback"
        )

        // Then
        val url = oneDriveFlow.getAuthorizationUrl()
        assertTrue(url.contains("login.microsoftonline.com"), "OneDrive flow should use Microsoft URL")
        assertTrue(url.contains("client_id=onedrive-client-id"))
    }

    // ==================== Edge Cases ====================

    @Test
    fun `getAuthorizationUrl should handle special characters in state`() {
        // Given
        val state = "state_with_special-chars.123"

        // When
        val url = oAuth2Flow.getAuthorizationUrl(state = state)

        // Then
        assertTrue(url.contains("state="))
    }

    @Test
    fun `multiple getAuthorizationUrl calls should be consistent`() {
        // When
        val url1 = oAuth2Flow.getAuthorizationUrl(state = "same-state")
        val url2 = oAuth2Flow.getAuthorizationUrl(state = "same-state")

        // Then
        assertEquals(url1, url2, "Same parameters should produce same URL")
    }

    @Test
    fun `TokenResponse equality should work correctly`() {
        // Given
        val response1 = TokenResponse(
            access_token = "token1",
            token_type = "Bearer",
            expires_in = 3600L,
            refresh_token = "refresh1",
            scope = "read"
        )
        val response2 = TokenResponse(
            access_token = "token1",
            token_type = "Bearer",
            expires_in = 3600L,
            refresh_token = "refresh1",
            scope = "read"
        )

        // Then
        assertEquals(response1, response2)
    }

    @Test
    fun `TokenResponse copy should work correctly`() {
        // Given
        val original = TokenResponse(
            access_token = "original_token",
            expires_in = 3600L,
            refresh_token = "original_refresh"
        )

        // When
        val copied = original.copy(access_token = "new_token")

        // Then
        assertEquals("new_token", copied.access_token)
        assertEquals(3600L, copied.expires_in)
        assertEquals("original_refresh", copied.refresh_token)
    }

    @Test
    fun `OAuth2Flow should accept single scope`() {
        // Given
        val httpClient = createHttpClient()
        val flowWithSingleScope = OAuth2Flow(
            httpClient = httpClient,
            clientId = testClientId,
            clientSecret = testClientSecret,
            authorizationUrl = testAuthorizationUrl,
            tokenUrl = testTokenUrl,
            redirectUri = testRedirectUri,
            scopes = listOf("single_scope")
        )

        // When
        val url = flowWithSingleScope.getAuthorizationUrl()

        // Then
        assertTrue(url.contains("scope="), "URL should contain scope parameter")
    }

    @Test
    fun `OAuth2Flow should handle many scopes`() {
        // Given
        val httpClient = createHttpClient()
        val manyScopes = (1..20).map { "scope_$it" }
        val flowWithManyScopes = OAuth2Flow(
            httpClient = httpClient,
            clientId = testClientId,
            clientSecret = testClientSecret,
            authorizationUrl = testAuthorizationUrl,
            tokenUrl = testTokenUrl,
            redirectUri = testRedirectUri,
            scopes = manyScopes
        )

        // When
        val url = flowWithManyScopes.getAuthorizationUrl()

        // Then
        assertTrue(url.contains("scope="), "URL should contain scope parameter")
    }
}
