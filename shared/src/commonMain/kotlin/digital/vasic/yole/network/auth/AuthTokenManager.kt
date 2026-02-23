package digital.vasic.yole.network.auth

import digital.vasic.yole.network.platform.SecureStorage
import digital.vasic.yole.network.platform.SecureStorageFactory
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Manages authentication tokens for cloud storage services.
 * Handles token storage, refresh, and expiration checking.
 */
class AuthTokenManager(
    private val serviceName: String,
    private val secureStorage: SecureStorage? = null
) {
    private val mutex = Mutex()
    private var _secureStorage: SecureStorage? = secureStorage
    
    /**
     * Get the secure storage instance, creating it if necessary
     */
    private suspend fun getSecureStorage(): SecureStorage {
        return _secureStorage ?: SecureStorageFactory.create().getOrThrow().also {
            _secureStorage = it
        }
    }
    
    // ==================== Internal (non-locking) methods ====================
    // These methods do NOT acquire the mutex and must only be called from within
    // a mutex.withLock block. This prevents deadlocks from nested lock acquisition.

    private suspend fun storeAccessTokenInternal(storage: SecureStorage, token: String): Result<Unit> {
        return storage.storeToken("${serviceName}_access", token)
    }

    private suspend fun storeRefreshTokenInternal(storage: SecureStorage, token: String): Result<Unit> {
        return storage.storeToken("${serviceName}_refresh", token)
    }

    private suspend fun storeTokenExpirationInternal(storage: SecureStorage, expiresAt: Instant): Result<Unit> {
        return storage.store("${serviceName}_expires", expiresAt.toEpochMilliseconds().toString())
    }

    private suspend fun getAccessTokenInternal(storage: SecureStorage): Result<String?> {
        return storage.retrieveToken("${serviceName}_access")
    }

    private suspend fun getRefreshTokenInternal(storage: SecureStorage): Result<String?> {
        return storage.retrieveToken("${serviceName}_refresh")
    }

    private suspend fun isTokenExpiredInternal(storage: SecureStorage): Result<Boolean> {
        val expiresAtStr = storage.retrieve("${serviceName}_expires").getOrNull()

        return if (expiresAtStr == null) {
            Result.success(true) // No expiration info, assume expired
        } else {
            val expiresAt = Instant.fromEpochMilliseconds(expiresAtStr.toLong())
            val now = Clock.System.now()
            Result.success(now >= expiresAt)
        }
    }

    // ==================== Public (locking) methods ====================

    /**
     * Store access token securely
     */
    suspend fun storeAccessToken(token: String): Result<Unit> {
        return mutex.withLock {
            try {
                val storage = getSecureStorage()
                storeAccessTokenInternal(storage, token)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Store refresh token securely
     */
    suspend fun storeRefreshToken(token: String): Result<Unit> {
        return mutex.withLock {
            try {
                val storage = getSecureStorage()
                storeRefreshTokenInternal(storage, token)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Store token expiration time
     */
    suspend fun storeTokenExpiration(expiresAt: Instant): Result<Unit> {
        return mutex.withLock {
            try {
                val storage = getSecureStorage()
                storeTokenExpirationInternal(storage, expiresAt)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Retrieve access token
     */
    suspend fun getAccessToken(): Result<String?> {
        return mutex.withLock {
            try {
                val storage = getSecureStorage()
                getAccessTokenInternal(storage)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Retrieve refresh token
     */
    suspend fun getRefreshToken(): Result<String?> {
        return mutex.withLock {
            try {
                val storage = getSecureStorage()
                getRefreshTokenInternal(storage)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Check if token is expired
     */
    suspend fun isTokenExpired(): Result<Boolean> {
        return mutex.withLock {
            try {
                val storage = getSecureStorage()
                isTokenExpiredInternal(storage)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Check if we have a valid access token
     */
    suspend fun hasValidToken(): Result<Boolean> {
        return mutex.withLock {
            try {
                val storage = getSecureStorage()
                val accessTokenResult = getAccessTokenInternal(storage)
                if (accessTokenResult.isFailure) return@withLock Result.success(false)

                val accessToken = accessTokenResult.getOrNull()
                if (accessToken.isNullOrBlank()) return@withLock Result.success(false)

                val isExpired = isTokenExpiredInternal(storage).getOrNull() ?: true
                Result.success(!isExpired)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Clear all tokens for this service
     */
    suspend fun clearTokens(): Result<Unit> {
        return mutex.withLock {
            try {
                val storage = getSecureStorage()
                storage.deleteToken("${serviceName}_access")
                storage.deleteToken("${serviceName}_refresh")
                storage.delete("${serviceName}_expires")
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Store complete token information
     */
    suspend fun storeTokenInfo(
        accessToken: String,
        refreshToken: String? = null,
        expiresIn: Long? = null
    ): Result<Unit> {
        return mutex.withLock {
            try {
                val storage = getSecureStorage()
                val results = mutableListOf<Result<Unit>>()

                // Store access token (using internal method to avoid nested lock)
                results.add(storeAccessTokenInternal(storage, accessToken))

                // Store refresh token if provided
                refreshToken?.let {
                    results.add(storeRefreshTokenInternal(storage, it))
                }

                // Store expiration if provided
                expiresIn?.let {
                    val expiresAt = Clock.System.now().plus(it.seconds)
                    results.add(storeTokenExpirationInternal(storage, expiresAt))
                }

                // Return first failure or success
                results.firstOrNull { it.isFailure } ?: Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get token info for debugging (without sensitive data)
     */
    suspend fun getTokenInfo(): Result<TokenInfo> {
        return mutex.withLock {
            try {
                val storage = getSecureStorage()
                val hasAccessToken = getAccessTokenInternal(storage).getOrNull()?.isNotBlank() ?: false
                val hasRefreshToken = getRefreshTokenInternal(storage).getOrNull()?.isNotBlank() ?: false
                val isExpired = isTokenExpiredInternal(storage).getOrNull() ?: true

                Result.success(
                    TokenInfo(
                        hasAccessToken = hasAccessToken,
                        hasRefreshToken = hasRefreshToken,
                        isExpired = isExpired,
                        serviceName = serviceName
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

/**
 * Token information for debugging and monitoring
 */
data class TokenInfo(
    val hasAccessToken: Boolean,
    val hasRefreshToken: Boolean,
    val isExpired: Boolean,
    val serviceName: String,
    val timestamp: Instant = Clock.System.now()
)