package digital.vasic.yole.network.platform

import kotlinx.browser.localStorage

/**
 * Web implementation of SecureStorageFactory.
 */
actual object SecureStorageFactory {

    /**
     * Create a new web secure storage instance.
     */
    actual suspend fun create(): Result<SecureStorage> {
        return try {
            // Check if localStorage is available
            if (localStorage == null) {
                return Result.failure(Exception("localStorage not available"))
            }

            val secureStorage = WebSecureStorage()
            Result.success(secureStorage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if secure storage is available on web platform.
     */
    actual suspend fun isAvailable(): Boolean {
        return try {
            // localStorage should be available in most modern browsers
            localStorage != null
        } catch (e: Exception) {
            false
        }
    }
}