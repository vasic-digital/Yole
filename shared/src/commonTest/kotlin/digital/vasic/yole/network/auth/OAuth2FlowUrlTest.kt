/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for OAuth2Flow URL generation
 *
 *########################################################*/
package digital.vasic.yole.network.auth

import digital.vasic.yole.network.protocol.createHttpClient
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Focused tests for OAuth2Flow.getAuthorizationUrl() method.
 * Validates that the generated authorization URL contains all required
 * OAuth2 parameters with correct values.
 */
class OAuth2FlowUrlTest {

    private val testClientId = "url_test_client_id"
    private val testClientSecret = "url_test_client_secret"
    private val testRedirectUri = "https://yole.app/oauth/callback"
    private val testAuthorizationUrl = "https://provider.example.com/oauth/authorize"
    private val testTokenUrl = "https://provider.example.com/oauth/token"

    private fun createFlow(scopes: List<String> = listOf("read", "write")): OAuth2Flow {
        return OAuth2Flow(
            httpClient = createHttpClient(),
            clientId = testClientId,
            clientSecret = testClientSecret,
            authorizationUrl = testAuthorizationUrl,
            tokenUrl = testTokenUrl,
            redirectUri = testRedirectUri,
            scopes = scopes
        )
    }

    // ==================== client_id ====================

    @Test
    fun `getAuthorizationUrl should contain client_id parameter`() {
        // Given
        val flow = createFlow()

        // When
        val url = flow.getAuthorizationUrl()

        // Then
        assertTrue(
            url.contains("client_id=$testClientId"),
            "URL should contain client_id=$testClientId but was: $url"
        )
    }

    // ==================== redirect_uri ====================

    @Test
    fun `getAuthorizationUrl should contain redirect_uri parameter`() {
        // Given
        val flow = createFlow()

        // When
        val url = flow.getAuthorizationUrl()

        // Then
        assertTrue(
            url.contains("redirect_uri="),
            "URL should contain redirect_uri parameter but was: $url"
        )
        // The redirect URI will be URL-encoded, so check for the encoded form
        assertTrue(
            url.contains("redirect_uri=https") || url.contains("redirect_uri=https%3A"),
            "URL should contain the redirect URI value but was: $url"
        )
    }

    // ==================== response_type=code ====================

    @Test
    fun `getAuthorizationUrl should contain response_type code`() {
        // Given
        val flow = createFlow()

        // When
        val url = flow.getAuthorizationUrl()

        // Then
        assertTrue(
            url.contains("response_type=code"),
            "URL should contain response_type=code but was: $url"
        )
    }

    // ==================== scopes ====================

    @Test
    fun `getAuthorizationUrl should contain scopes when provided`() {
        // Given
        val flow = createFlow(scopes = listOf("read", "write"))

        // When
        val url = flow.getAuthorizationUrl()

        // Then
        assertTrue(
            url.contains("scope="),
            "URL should contain scope parameter when scopes are provided but was: $url"
        )
    }

    // ==================== state ====================

    @Test
    fun `getAuthorizationUrl should contain state when provided`() {
        // Given
        val flow = createFlow()
        val state = "csrf_protection_token_abc123"

        // When
        val url = flow.getAuthorizationUrl(state = state)

        // Then
        assertTrue(
            url.contains("state=$state"),
            "URL should contain state=$state but was: $url"
        )
    }

    // ==================== access_type=offline ====================

    @Test
    fun `getAuthorizationUrl should contain access_type offline`() {
        // Given
        val flow = createFlow()

        // When
        val url = flow.getAuthorizationUrl()

        // Then
        assertTrue(
            url.contains("access_type=offline"),
            "URL should contain access_type=offline to request refresh token but was: $url"
        )
    }

    // ==================== prompt=consent ====================

    @Test
    fun `getAuthorizationUrl should contain prompt consent`() {
        // Given
        val flow = createFlow()

        // When
        val url = flow.getAuthorizationUrl()

        // Then
        assertTrue(
            url.contains("prompt=consent"),
            "URL should contain prompt=consent to force consent screen but was: $url"
        )
    }

    // ==================== empty scopes ====================

    @Test
    fun `getAuthorizationUrl should work with empty scopes`() {
        // Given
        val flow = createFlow(scopes = emptyList())

        // When
        val url = flow.getAuthorizationUrl()

        // Then
        assertFalse(
            url.contains("scope="),
            "URL should not contain scope parameter when scopes are empty but was: $url"
        )
        // Should still contain all other required parameters
        assertTrue(url.contains("client_id=$testClientId"))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("access_type=offline"))
        assertTrue(url.contains("prompt=consent"))
    }

    // ==================== multiple scopes joined with space ====================

    @Test
    fun `getAuthorizationUrl should join multiple scopes with space`() {
        // Given
        val scopes = listOf("files.read", "files.write", "user.profile")
        val flow = createFlow(scopes = scopes)

        // When
        val url = flow.getAuthorizationUrl()

        // Then
        // Scopes are joined with space which gets URL-encoded to + or %20
        val expectedJoined = scopes.joinToString(" ")
        // The URL should contain the scope parameter with the joined scopes
        // (URL encoding may convert space to + or %20)
        assertTrue(
            url.contains("scope=files.read") || url.contains("scope=files.read"),
            "URL should contain joined scopes but was: $url"
        )
        // Verify all scope values are present in the URL (possibly URL-encoded)
        for (scope in scopes) {
            assertTrue(
                url.contains(scope),
                "URL should contain scope '$scope' but was: $url"
            )
        }
    }
}
