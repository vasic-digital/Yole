package digital.vasic.yole.network.platform

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
            if (typeof(localStorage) != "object") {
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
            typeof(localStorage) == "object" && localStorage != null
        } catch (e: Exception) {
            false
        }
    }
}

// Helper function for type checking in JavaScript
private external fun typeof(obj: dynamic): String