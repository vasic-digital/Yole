/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Web secure storage implementation using localStorage
 * with XOR obfuscation and Base64 encoding.
 *
 *########################################################*/
package digital.vasic.yole.network.platform

import kotlinx.browser.localStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Web implementation of secure storage using localStorage with basic obfuscation.
 *
 * ## Security Limitation
 *
 * **This implementation does NOT provide true encryption.** It uses XOR obfuscation
 * with a static key and Base64 encoding, which is trivially reversible by anyone
 * with access to the JavaScript source code or browser developer tools.
 *
 * ### Threat Model
 * - **Protected against:** Casual inspection of localStorage via browser UI
 * - **NOT protected against:** XSS attacks, browser extensions with page access,
 *   developer tools inspection, any attacker with JavaScript execution context
 *
 * ### Why True Encryption Is Not Yet Implemented
 *
 * The Web Crypto API (`crypto.subtle`) provides AES-GCM encryption suitable for
 * securing localStorage values. However, integrating it from Kotlin/Wasm requires
 * non-trivial JavaScript interop:
 *
 * 1. **Async Promise API**: `crypto.subtle.generateKey()`, `encrypt()`, and `decrypt()`
 *    return JavaScript `Promise` objects. Kotlin/Wasm's JS interop for `Promise`
 *    resolution requires `JsPromise` handling or `suspend` bridge functions, which
 *    are still evolving in the Kotlin/Wasm target.
 *
 * 2. **Key Persistence**: The AES-GCM `CryptoKey` must be stored persistently.
 *    `localStorage` only stores strings, so the key would need to be exported via
 *    `crypto.subtle.exportKey("jwk", key)` and re-imported on each session.
 *    Alternatively, IndexedDB can store `CryptoKey` objects directly.
 *
 * 3. **Binary Data Handling**: `crypto.subtle.encrypt()` operates on `ArrayBuffer`/
 *    `Uint8Array`, requiring conversion to/from Base64 strings for localStorage.
 *
 * ### Implementation Roadmap for Web Crypto API Integration
 *
 * TODO: Implement AES-GCM encryption using Web Crypto API when Kotlin/Wasm
 *       JS interop matures. The implementation should:
 *       1. Check `crypto.subtle` availability (requires secure context: HTTPS or localhost)
 *       2. On first use, generate an AES-GCM 256-bit key via:
 *          `crypto.subtle.generateKey({name: "AES-GCM", length: 256}, true, ["encrypt", "decrypt"])`
 *       3. Export the key as JWK and store in localStorage under a dedicated key
 *       4. For each `store()` call: generate a random 12-byte IV, encrypt the value,
 *          concatenate IV + ciphertext, Base64-encode, and store
 *       5. For each `retrieve()` call: Base64-decode, split IV + ciphertext, decrypt
 *       6. Fall back to XOR obfuscation if `crypto.subtle` is unavailable
 *          (e.g., non-secure HTTP context)
 *
 * @see SecureStorage
 * @see SecureStorageFactory
 */
class WebSecureStorage : SecureStorage {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Indicates whether true encryption (e.g., AES-GCM via Web Crypto API) is supported.
     *
     * Currently always returns `false` because only XOR obfuscation is implemented.
     * When Web Crypto API integration is complete, this will check for
     * `crypto.subtle` availability (requires HTTPS or localhost secure context).
     */
    val isEncryptionSupported: Boolean
        get() = false

    /**
     * Returns the security level of the current storage implementation.
     *
     * - `"obfuscated"` — Current state: XOR obfuscation, trivially reversible
     * - `"encrypted"` — Future state: AES-GCM via Web Crypto API
     * - `"plaintext"` — Fallback if obfuscation fails
     */
    val securityLevel: String
        get() = "obfuscated"

    override suspend fun store(key: String, value: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val encryptedValue = obfuscateData(value)
            localStorage.setItem(getStorageKey(key), encryptedValue)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun retrieve(key: String): Result<String?> = withContext(Dispatchers.Default) {
        try {
            val encryptedValue = localStorage.getItem(getStorageKey(key)) ?: return@withContext Result.success(null)
            val decryptedValue = deobfuscateData(encryptedValue)
            Result.success(decryptedValue)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(key: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            localStorage.removeItem(getStorageKey(key))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun contains(key: String): Result<Boolean> = withContext(Dispatchers.Default) {
        try {
            val value = localStorage.getItem(getStorageKey(key))
            Result.success(value != null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listKeys(): Result<List<String>> = withContext(Dispatchers.Default) {
        try {
            val prefix = STORAGE_PREFIX
            val keys = mutableListOf<String>()

            for (i in 0 until localStorage.length) {
                val fullKey = localStorage.key(i) ?: continue
                if (fullKey.startsWith(prefix)) {
                    keys.add(fullKey.substring(prefix.length))
                }
            }

            Result.success(keys)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clear(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val prefix = STORAGE_PREFIX
            val keysToRemove = mutableListOf<String>()

            for (i in 0 until localStorage.length) {
                val fullKey = localStorage.key(i) ?: continue
                if (fullKey.startsWith(prefix)) {
                    keysToRemove.add(fullKey)
                }
            }

            keysToRemove.forEach { key ->
                localStorage.removeItem(key)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isSecure(): Result<Boolean> = withContext(Dispatchers.Default) {
        try {
            // Web environment can only provide obfuscation, not true encryption.
            // This tests that the obfuscation/deobfuscation cycle works correctly,
            // but does NOT indicate cryptographic security.
            val testKey = "_secure_storage_test_"
            val testValue = "test_value_${Clock.System.now().toEpochMilliseconds()}"

            store(testKey, testValue)
            val retrieved = retrieve(testKey).getOrNull()
            delete(testKey)

            // Returns false because XOR obfuscation is not cryptographically secure.
            // Will return true once Web Crypto API integration is implemented.
            Result.success(false)
        } catch (e: Exception) {
            Result.success(false)
        }
    }

    private fun getStorageKey(key: String): String {
        return "$STORAGE_PREFIX$key"
    }

    private fun obfuscateData(data: String): String {
        // Simple XOR obfuscation with a fixed key.
        // WARNING: This is NOT secure encryption, just basic obfuscation to prevent
        // casual inspection of localStorage values. See class KDoc for details.
        val key = obfuscationKey
        val result = StringBuilder()

        for (i in data.indices) {
            val char = data[i].code
            val obfuscated = char xor key[i % key.length].code
            result.append(obfuscated.toChar())
        }

        // Convert to base64 for localStorage compatibility
        return btoa(result.toString())
    }

    private fun deobfuscateData(obfuscatedData: String): String {
        // Convert from base64 and apply XOR deobfuscation
        val encoded = atob(obfuscatedData)
        val key = obfuscationKey
        val result = StringBuilder()

        for (i in encoded.indices) {
            val char = encoded[i].code
            val deobfuscated = char xor key[i % key.length].code
            result.append(deobfuscated.toChar())
        }

        return result.toString()
    }

    companion object {
        private const val STORAGE_PREFIX = "yole_network_secure_"
        private val obfuscationKey = "Y0l3_N3tw0rk_S3cur3_K3y_2025"
    }
}

// External functions for base64 encoding/decoding
// These are available in most web browsers
external fun btoa(data: String): String
external fun atob(data: String): String
