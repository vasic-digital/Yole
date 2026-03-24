/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Web/Wasm implementation of SecureStorageFactory.
 * Uses AES-GCM encryption via Web Crypto API in secure
 * contexts, falling back to XOR obfuscation otherwise.
 *
 *########################################################*/
package digital.vasic.yole.network.platform

import kotlinx.browser.localStorage

/**
 * Web implementation of [SecureStorageFactory].
 *
 * Creates [WebSecureStorage] instances backed by localStorage. In secure contexts
 * (HTTPS or localhost), values are encrypted with AES-GCM via the Web Crypto API.
 * In non-secure contexts, falls back to XOR obfuscation with a console warning.
 * See [WebSecureStorage] for the full security model.
 *
 * ## Security Considerations
 *
 * - In secure contexts: AES-GCM 256-bit encryption via `crypto.subtle`
 * - In non-secure contexts: XOR obfuscation fallback (trivially reversible)
 * - localStorage is subject to the same-origin policy, providing domain-level isolation.
 * - localStorage has a ~5-10 MB size limit depending on the browser.
 * - The AES-GCM key is persisted as JWK in localStorage under `yole_crypto_key`.
 *
 * ## Future Enhancements
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
     * Returns `true` if localStorage is accessible. Check
     * [WebSecureStorage.isEncryptionSupported] to determine whether AES-GCM encryption
     * is active (secure context) or XOR obfuscation fallback is in use.
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
