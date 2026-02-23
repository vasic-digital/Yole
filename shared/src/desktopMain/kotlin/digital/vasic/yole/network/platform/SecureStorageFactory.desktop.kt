package digital.vasic.yole.network.platform

import java.io.File
import java.nio.file.Paths

/**
 * Desktop implementation of SecureStorageFactory.
 */
actual object SecureStorageFactory {
    
    /**
     * Create a new desktop secure storage instance.
     */
    actual suspend fun create(): Result<SecureStorage> {
        return try {
            val userHome = System.getProperty("user.home") ?: System.getenv("HOME") ?: "."
            val storageDir = File(Paths.get(userHome, ".yole", "secure").toString())
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            val secureStorage = DesktopSecureStorage(storageDir)
            Result.success(secureStorage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if secure storage is available on desktop platforms.
     */
    actual suspend fun isAvailable(): Boolean {
        return try {
            // Desktop platforms should always have access to file system and crypto
            // Check if we can create the storage directory
            val userHome = System.getProperty("user.home") ?: System.getenv("HOME") ?: "."
            val storageDir = File(Paths.get(userHome, ".yole", "secure").toString())
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            storageDir.exists() && storageDir.canWrite()
        } catch (e: Exception) {
            false
        }
    }
}