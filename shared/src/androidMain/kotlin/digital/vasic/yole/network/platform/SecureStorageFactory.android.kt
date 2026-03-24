/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Android implementation of SecureStorageFactory.
 * Uses EncryptedSharedPreferences for real credential encryption.
 *
 *########################################################*/
package digital.vasic.yole.network.platform

import android.content.Context

/**
 * Android implementation of SecureStorageFactory.
 *
 * Requires [initialize] to be called with an Android [Context] before [create]
 * is invoked. The context is stored as the application context to avoid leaking
 * Activity references.
 */
actual object SecureStorageFactory {

    @Volatile
    private var appContext: Context? = null

    /**
     * Initialize the factory with an Android application context.
     * Must be called once during app startup (e.g., in Application.onCreate
     * or the main Activity) before any SecureStorage operations.
     *
     * @param context Any Android context; the application context is retained.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Create a new Android secure storage instance backed by EncryptedSharedPreferences.
     *
     * @return [Result.success] with an [AndroidSecureStorage] if the context has been
     *         initialized via [initialize], or [Result.failure] if not yet initialized.
     */
    actual suspend fun create(): Result<SecureStorage> {
        return try {
            val context = appContext
                ?: return Result.failure(
                    IllegalStateException(
                        "SecureStorageFactory not initialized. " +
                        "Call SecureStorageFactory.initialize(context) during app startup."
                    )
                )
            val secureStorage = AndroidSecureStorage(context)
            Result.success(secureStorage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if secure storage is available on Android.
     * Returns true if the factory has been initialized with a context.
     */
    actual suspend fun isAvailable(): Boolean {
        return appContext != null
    }
}
