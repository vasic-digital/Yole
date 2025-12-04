package digital.vasic.yole.network.platform

import android.content.Context
import digital.vasic.yole.YoleApplication

/**
 * Android implementation of SecureStorageFactory.
 */
actual object SecureStorageFactory {
    
    /**
     * Create a new Android secure storage instance.
     */
    actual suspend fun create(): Result<SecureStorage> {
        return try {
            val context = YoleApplication.instance.applicationContext
            val secureStorage = AndroidSecureStorage(context)
            Result.success(secureStorage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if secure storage is available on Android.
     */
    actual suspend fun isAvailable(): Boolean {
        return try {
            // Android always supports secure storage via EncryptedSharedPreferences
            // but we should check if the security crypto module is available
            val context = YoleApplication.instance.applicationContext
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            true
        } catch (e: Exception) {
            false
        }
    }
}