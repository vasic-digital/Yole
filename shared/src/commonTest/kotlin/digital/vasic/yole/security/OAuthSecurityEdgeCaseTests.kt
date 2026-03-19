/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * OAuth2 security edge-case tests.
 * Validates AuthTokenManager and SecureStorage behaviour
 * with null, empty, expired, malformed, and concurrent
 * token access patterns.
 *
 *########################################################*/
package digital.vasic.yole.security

import digital.vasic.yole.network.auth.AuthTokenManager
import digital.vasic.yole.network.auth.TokenInfo
import digital.vasic.yole.network.platform.SecureStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * OAuth2 security edge-case tests exercising AuthTokenManager
 * and SecureStorage with adversarial inputs and concurrent access.
 *
 * Categories:
 * - Null/empty/blank token handling
 * - Expired token detection
 * - Malformed token strings
 * - Concurrent token access (50 coroutines)
 * - Token storage isolation between services
 * - toString() credential leak prevention
 * - SecureStorage store/retrieve/delete roundtrip
 * - Error recovery and boundary conditions
 *
 * Total: 30+ tests
 */
class OAuthSecurityEdgeCaseTests {

    private lateinit var storage: InMemorySecureStorage
    private lateinit var manager: AuthTokenManager

    @BeforeTest
    fun setUp() {
        storage = InMemorySecureStorage()
        manager = AuthTokenManager("oauth_test", storage)
    }

    @AfterTest
    fun tearDown() = runBlocking<Unit> {
        storage.clear()
    }

    // ==================== Empty/Blank Token Handling ====================

    @Test
    fun storeEmptyAccessToken() = runBlocking<Unit> {
        val result = manager.storeAccessToken("")
        assertTrue(result.isSuccess, "Empty token should be storable without crash")
    }

    @Test
    fun storeBlankAccessToken() = runBlocking<Unit> {
        val result = manager.storeAccessToken("   ")
        assertTrue(result.isSuccess, "Blank token should be storable without crash")
    }

    @Test
    fun retrieveNonExistentAccessToken() = runBlocking<Unit> {
        val result = manager.getAccessToken()
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull(), "Non-existent token should return null")
    }

    @Test
    fun storeEmptyRefreshToken() = runBlocking<Unit> {
        val result = manager.storeRefreshToken("")
        assertTrue(result.isSuccess)
    }

    @Test
    fun hasValidTokenReturnsFalseForEmptyToken() = runBlocking<Unit> {
        manager.storeAccessToken("")
        val result = manager.hasValidToken()
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true,
            "Empty access token should not be considered valid")
    }

    @Test
    fun hasValidTokenReturnsFalseForBlankToken() = runBlocking<Unit> {
        manager.storeAccessToken("   ")
        val result = manager.hasValidToken()
        assertTrue(result.isSuccess)
        // A blank token is not null/blank-checked in storeAccessToken,
        // but hasValidToken checks isNotBlank
        assertFalse(result.getOrNull() ?: true,
            "Blank access token should not be valid")
    }

    // ==================== Expired Token Detection ====================

    @Test
    fun tokenExpirationInThePast() = runBlocking<Unit> {
        manager.storeAccessToken("valid-token")
        val pastInstant = Clock.System.now().minus(1.hours)
        manager.storeTokenExpiration(pastInstant)

        val expired = manager.isTokenExpired()
        assertTrue(expired.isSuccess)
        assertTrue(expired.getOrNull() ?: false,
            "Token with past expiration should be expired")
    }

    @Test
    fun tokenExpirationExactlyNow() = runBlocking<Unit> {
        manager.storeAccessToken("valid-token")
        val now = Clock.System.now()
        manager.storeTokenExpiration(now)

        val expired = manager.isTokenExpired()
        assertTrue(expired.isSuccess)
        assertTrue(expired.getOrNull() ?: false,
            "Token expiring at current instant should be expired")
    }

    @Test
    fun tokenExpirationFarFuture() = runBlocking<Unit> {
        manager.storeAccessToken("valid-token")
        val futureInstant = Clock.System.now().plus(24.hours)
        manager.storeTokenExpiration(futureInstant)

        val expired = manager.isTokenExpired()
        assertTrue(expired.isSuccess)
        assertFalse(expired.getOrNull() ?: true,
            "Token expiring far in the future should NOT be expired")
    }

    @Test
    fun hasValidTokenReturnsFalseForExpiredToken() = runBlocking<Unit> {
        manager.storeAccessToken("valid-token")
        val pastInstant = Clock.System.now().minus(1.hours)
        manager.storeTokenExpiration(pastInstant)

        val result = manager.hasValidToken()
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true,
            "Expired token should not be valid")
    }

    @Test
    fun noExpirationStoredTreatedAsExpired() = runBlocking<Unit> {
        manager.storeAccessToken("token-no-expiry")
        // Do NOT store expiration

        val expired = manager.isTokenExpired()
        assertTrue(expired.isSuccess)
        assertTrue(expired.getOrNull() ?: false,
            "Token with no expiration info should be treated as expired")
    }

    // ==================== Malformed Token Strings ====================

    @Test
    fun tokenWithNullBytes() = runBlocking<Unit> {
        val token = "token\u0000with\u0000nulls"
        val result = manager.storeAccessToken(token)
        assertTrue(result.isSuccess)
    }

    @Test
    fun tokenWithControlCharacters() = runBlocking<Unit> {
        val token = "token\u0001\u0002\u0003\u001B\u007F"
        val result = manager.storeAccessToken(token)
        assertTrue(result.isSuccess)
    }

    @Test
    fun veryLongToken() = runBlocking<Unit> {
        val token = "t".repeat(10_000)
        val result = manager.storeAccessToken(token)
        assertTrue(result.isSuccess)
    }

    @Test
    fun tokenWithSpecialCharacters() = runBlocking<Unit> {
        val token = "eyJ!@#\$%^&*()_+-=[]{}|;':\",./<>?~`"
        val result = manager.storeAccessToken(token)
        assertTrue(result.isSuccess)
    }

    @Test
    fun tokenWithUnicodeCharacters() = runBlocking<Unit> {
        val token = "token-\u00E9\u00E0\u4E2D\u6587-\uD83D\uDE00"
        val result = manager.storeAccessToken(token)
        assertTrue(result.isSuccess)

        val retrieved = manager.getAccessToken()
        assertTrue(retrieved.isSuccess)
    }

    @Test
    fun tokenWithNewlines() = runBlocking<Unit> {
        val token = "token\nwith\nnewlines\r\nand\rcarriage\rreturns"
        val result = manager.storeAccessToken(token)
        assertTrue(result.isSuccess)
    }

    // ==================== Concurrent Token Access ====================

    @Test
    fun concurrentTokenReads() = runBlocking<Unit> {
        manager.storeAccessToken("shared-token")
        val futureInstant = Clock.System.now().plus(1.hours)
        manager.storeTokenExpiration(futureInstant)

        val results = (1..50).map {
            async {
                manager.getAccessToken()
            }
        }.awaitAll()

        assertEquals(50, results.size)
        assertTrue(results.all { it.isSuccess },
            "All concurrent reads should succeed")
    }

    @Test
    fun concurrentTokenInfoReads() = runBlocking<Unit> {
        manager.storeTokenInfo(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresIn = 3600L
        )

        val results = (1..50).map {
            async {
                manager.getTokenInfo()
            }
        }.awaitAll()

        assertEquals(50, results.size)
        assertTrue(results.all { it.isSuccess })
        results.forEach { result ->
            val info = result.getOrNull()
            assertNotNull(info)
            assertTrue(info.hasAccessToken)
            assertTrue(info.hasRefreshToken)
        }
    }

    @Test
    fun concurrentHasValidTokenChecks() = runBlocking<Unit> {
        manager.storeAccessToken("valid-token")
        val futureInstant = Clock.System.now().plus(1.hours)
        manager.storeTokenExpiration(futureInstant)

        val results = (1..50).map {
            async {
                manager.hasValidToken()
            }
        }.awaitAll()

        assertEquals(50, results.size)
        assertTrue(results.all { it.isSuccess })
        assertTrue(results.all { it.getOrNull() == true },
            "All concurrent checks should see valid token")
    }

    @Test
    fun concurrentMixedReadWriteOperations() = runBlocking<Unit> {
        val results = (1..50).map { i ->
            async {
                if (i % 2 == 0) {
                    manager.storeAccessToken("token-$i")
                } else {
                    manager.getAccessToken()
                }
            }
        }.awaitAll()

        assertEquals(50, results.size)
        assertTrue(results.all { it.isSuccess },
            "All concurrent mixed operations should succeed without deadlock")
    }

    // ==================== Token Storage Isolation Between Services ====================

    @Test
    fun separateServicesHaveIsolatedTokens() = runBlocking<Unit> {
        val manager1 = AuthTokenManager("service_A", storage)
        val manager2 = AuthTokenManager("service_B", storage)

        manager1.storeAccessToken("token-A")
        manager2.storeAccessToken("token-B")

        val tokenA = manager1.getAccessToken().getOrNull()
        val tokenB = manager2.getAccessToken().getOrNull()

        assertNotNull(tokenA)
        assertNotNull(tokenB)
        assertNotEquals(tokenA, tokenB, "Tokens from different services must be isolated")
    }

    @Test
    fun clearTokensOnlyAffectsOwnService() = runBlocking<Unit> {
        val manager1 = AuthTokenManager("service_X", storage)
        val manager2 = AuthTokenManager("service_Y", storage)

        manager1.storeAccessToken("token-X")
        manager2.storeAccessToken("token-Y")

        manager1.clearTokens()

        val tokenX = manager1.getAccessToken().getOrNull()
        val tokenY = manager2.getAccessToken().getOrNull()

        assertNull(tokenX, "Cleared service token should be null")
        assertNotNull(tokenY, "Other service token should remain intact")
    }

    @Test
    fun manyServicesCanCoexist() = runBlocking<Unit> {
        val managers = (1..20).map { i ->
            AuthTokenManager("svc_$i", storage)
        }

        managers.forEachIndexed { i, mgr ->
            mgr.storeAccessToken("tok_$i")
        }

        managers.forEachIndexed { i, mgr ->
            val result = mgr.getAccessToken()
            assertTrue(result.isSuccess)
            assertNotNull(result.getOrNull())
        }
    }

    // ==================== toString() Credential Leak Prevention ====================

    @Test
    fun tokenInfoToStringDoesNotContainActualToken() {
        val info = TokenInfo(
            hasAccessToken = true,
            hasRefreshToken = true,
            isExpired = false,
            serviceName = "my_service"
        )
        val str = info.toString()
        assertNotNull(str)
        // TokenInfo stores boolean flags, not actual tokens, so it inherently
        // does not contain token values. Verify the design.
        assertFalse(str.contains("Bearer"), "TokenInfo toString should not contain token values")
        assertFalse(str.contains("eyJ"), "TokenInfo toString should not contain JWT-like values")
        assertTrue(str.contains("my_service"), "Should contain service name")
    }

    @Test
    fun tokenInfoOnlyContainsBooleanFlags() {
        val info = TokenInfo(
            hasAccessToken = true,
            hasRefreshToken = false,
            isExpired = true,
            serviceName = "test"
        )
        val str = info.toString()
        assertTrue(str.contains("hasAccessToken=true"))
        assertTrue(str.contains("hasRefreshToken=false"))
        assertTrue(str.contains("isExpired=true"))
    }

    // ==================== SecureStorage Contract Tests ====================

    @Test
    fun storeRetrieveRoundTrip() = runBlocking<Unit> {
        storage.store("key1", "value1")
        val result = storage.retrieve("key1")
        assertTrue(result.isSuccess)
        assertEquals("value1", result.getOrNull())
    }

    @Test
    fun storeDeleteRetrieveReturnsNull() = runBlocking<Unit> {
        storage.store("key1", "value1")
        storage.delete("key1")
        val result = storage.retrieve("key1")
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun storeOverwritesPreviousValue() = runBlocking<Unit> {
        storage.store("key1", "first")
        storage.store("key1", "second")
        val result = storage.retrieve("key1")
        assertEquals("second", result.getOrNull())
    }

    @Test
    fun containsReturnsTrueForExistingKey() = runBlocking<Unit> {
        storage.store("key1", "value1")
        val result = storage.contains("key1")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() ?: false)
    }

    @Test
    fun containsReturnsFalseForMissingKey() = runBlocking<Unit> {
        val result = storage.contains("nonexistent")
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() ?: true)
    }

    @Test
    fun listKeysReturnsAllStoredKeys() = runBlocking<Unit> {
        storage.store("alpha", "1")
        storage.store("beta", "2")
        storage.store("gamma", "3")
        val keys = storage.listKeys().getOrNull() ?: emptyList()
        assertTrue(keys.contains("alpha"))
        assertTrue(keys.contains("beta"))
        assertTrue(keys.contains("gamma"))
    }

    @Test
    fun clearRemovesAllEntries() = runBlocking<Unit> {
        storage.store("a", "1")
        storage.store("b", "2")
        storage.clear()
        val keys = storage.listKeys().getOrNull() ?: emptyList()
        assertTrue(keys.isEmpty(), "After clear, storage should be empty")
    }

    @Test
    fun isSecureReturnsTrue() = runBlocking<Unit> {
        val result = storage.isSecure()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() ?: false)
    }

    @Test
    fun credentialStoreRetrieveRoundTrip() = runBlocking<Unit> {
        storage.storeCredentials("myservice", "admin", "s3cret!")
        val creds = storage.retrieveCredentials("myservice")
        assertTrue(creds.isSuccess)
        val pair = creds.getOrNull()
        assertNotNull(pair)
        assertEquals("admin", pair.first)
        assertEquals("s3cret!", pair.second)
    }

    @Test
    fun credentialDeleteRemovesEntry() = runBlocking<Unit> {
        storage.storeCredentials("myservice", "admin", "pass")
        storage.deleteCredentials("myservice")
        val creds = storage.retrieveCredentials("myservice")
        assertTrue(creds.isSuccess)
        assertNull(creds.getOrNull())
    }

    @Test
    fun tokenStoreRetrieveRoundTrip() = runBlocking<Unit> {
        storage.storeToken("authservice", "tok_abc123")
        val tok = storage.retrieveToken("authservice")
        assertTrue(tok.isSuccess)
        assertEquals("tok_abc123", tok.getOrNull())
    }

    @Test
    fun tokenDeleteRemovesEntry() = runBlocking<Unit> {
        storage.storeToken("authservice", "tok_abc123")
        storage.deleteToken("authservice")
        val tok = storage.retrieveToken("authservice")
        assertTrue(tok.isSuccess)
        assertNull(tok.getOrNull())
    }

    // ==================== Error Recovery ====================

    @Test
    fun storageFailureThenRecovery() = runBlocking<Unit> {
        storage.shouldFail = true
        val failResult = manager.storeAccessToken("token")
        assertTrue(failResult.isFailure)

        storage.shouldFail = false
        val successResult = manager.storeAccessToken("token")
        assertTrue(successResult.isSuccess)
    }

    @Test
    fun getTokenInfoOnEmptyStorage() = runBlocking<Unit> {
        val result = manager.getTokenInfo()
        assertTrue(result.isSuccess)
        val info = result.getOrNull()
        assertNotNull(info)
        assertFalse(info.hasAccessToken)
        assertFalse(info.hasRefreshToken)
        assertTrue(info.isExpired, "No expiration info should mean expired")
    }

    @Test
    fun clearTokensOnEmptyStorage() = runBlocking<Unit> {
        val result = manager.clearTokens()
        assertTrue(result.isSuccess, "Clearing empty token store should succeed")
    }

    @Test
    fun storeTokenInfoWithMinimalData() = runBlocking<Unit> {
        val result = manager.storeTokenInfo(
            accessToken = "min-token"
        )
        assertTrue(result.isSuccess)
        val info = manager.getTokenInfo().getOrNull()
        assertNotNull(info)
        assertTrue(info.hasAccessToken)
        assertFalse(info.hasRefreshToken, "No refresh token was provided")
    }

    // ==================== Test Helper ====================

    private class InMemorySecureStorage : SecureStorage {
        private val data = mutableMapOf<String, String>()
        var shouldFail = false

        override suspend fun store(key: String, value: String): Result<Unit> {
            if (shouldFail) return Result.failure(Exception("Storage failed"))
            data[key] = value
            return Result.success(Unit)
        }

        override suspend fun retrieve(key: String): Result<String?> {
            if (shouldFail) return Result.failure(Exception("Storage failed"))
            return Result.success(data[key])
        }

        override suspend fun delete(key: String): Result<Unit> {
            if (shouldFail) return Result.failure(Exception("Storage failed"))
            data.remove(key)
            return Result.success(Unit)
        }

        override suspend fun contains(key: String): Result<Boolean> {
            if (shouldFail) return Result.failure(Exception("Storage failed"))
            return Result.success(data.containsKey(key))
        }

        override suspend fun listKeys(): Result<List<String>> {
            if (shouldFail) return Result.failure(Exception("Storage failed"))
            return Result.success(data.keys.toList())
        }

        override suspend fun clear(): Result<Unit> {
            data.clear()
            return Result.success(Unit)
        }

        override suspend fun isSecure(): Result<Boolean> {
            return Result.success(true)
        }
    }
}
