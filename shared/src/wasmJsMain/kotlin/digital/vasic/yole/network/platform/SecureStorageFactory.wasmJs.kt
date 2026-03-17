/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Web/Wasm implementation of SecureStorageFactory.
 * Uses localStorage with basic obfuscation (not true encryption).
 *
 *########################################################*/
package digital.vasic.yole.network.platform

import kotlinx.browser.localStorage

/**
 * Web implementation of [SecureStorageFactory].
 *
 * Creates [WebSecureStorage] instances backed by localStorage with XOR obfuscation.
 * This provides data hiding but NOT cryptographic security. See [WebSecureStorage]
 * for a detailed security analysis and the Web Crypto API implementation roadmap.
 *
 * ## Security Considerations
 *
 * - Values are XOR-obfuscated, not encrypted. This prevents casual inspection
 *   but is trivially reversible by an attacker with JavaScript execution context.
 * - localStorage is subject to the same-origin policy, providing domain-level isolation.
 * - localStorage has a ~5-10 MB size limit depending on the browser.
 * - localStorage is synchronous and blocks the main thread; consider IndexedDB
 *   for large or frequent storage operations.
 *
 * ## Future Enhancements
 *
 * TODO: When [WebSecureStorage] gains Web Crypto API support, this factory should:
 *       1. Check if `crypto.subtle` is available (HTTPS/localhost secure context)
 *       2. If available, return a `CryptoSecureStorage` instance with AES-GCM encryption
 *       3. If unavailable, fall back to the current `WebSecureStorage` with obfuscation
 *       4. Log a warning when falling back to obfuscation-only mode
 *
 * TODO: Consider using IndexedDB instead of localStorage for:
 *       - Larger storage capacity (browser-dependent, typically 50+ MB)
 *       - Async API (non-blocking)
 *       - Ability to store CryptoKey objects directly without export/import
 */
actual object SecureStorageFactory {

    /**
     * Create a new web secure storage instance.
     *
     * @return [Result.success] with a [WebSecureStorage] if localStorage is available,
     *         or [Result.failure] if the browser environment does not support localStorage
     *         (e.g., private browsing in some older browsers, or server-side rendering).
     */
    actual suspend fun create(): Result<SecureStorage> {
        return try {
            // Check if localStorage is available
            if (localStorage == null) {
                return Result.failure(
                    IllegalStateException(
                        "localStorage is not available in this browser environment. " +
                        "This may occur in private browsing mode on some older browsers, " +
                        "or in server-side rendering contexts."
                    )
                )
            }

            val secureStorage = WebSecureStorage()
            Result.success(secureStorage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if secure storage is available on web platform.
     *
     * Returns `true` if localStorage is accessible. Note that this does NOT indicate
     * that true encryption is available — only that the obfuscation-based storage
     * can be used. Check [WebSecureStorage.isEncryptionSupported] for encryption status.
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
