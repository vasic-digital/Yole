/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Web/Wasm implementation of SecureStorageFactory
 * Uses localStorage with basic obfuscation (not true encryption)
 *
 *########################################################*/
package digital.vasic.yole.network.platform

import kotlinx.browser.localStorage

/**
 * Web implementation of SecureStorageFactory.
 *
 * Uses [WebSecureStorage] backed by localStorage with XOR obfuscation.
 * This provides data hiding but not cryptographic security.
 *
 * TODO: Evaluate using the Web Crypto API (SubtleCrypto) for AES-GCM encryption
 *       of stored values. This would require:
 *       1. Generating an encryption key via crypto.subtle.generateKey()
 *       2. Storing the key in an IndexedDB (not localStorage) for persistence
 *       3. Encrypting/decrypting values via crypto.subtle.encrypt/decrypt
 *       4. Handling the async Promise-based API via JS interop
 *
 * TODO: Consider using IndexedDB instead of localStorage for larger storage capacity
 *       and structured data support.
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