/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Web secure storage implementation using localStorage
 * with AES-GCM encryption via Web Crypto API, falling
 * back to XOR obfuscation in non-secure contexts.
 *
 *########################################################*/
package digital.vasic.yole.network.platform

import kotlinx.browser.localStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Web implementation of secure storage using localStorage with AES-GCM encryption
 * via the Web Crypto API, falling back to XOR obfuscation in non-secure contexts.
 *
 * ## Security Model
 *
 * When running in a secure context (HTTPS or localhost), this implementation uses
 * AES-GCM 256-bit encryption via the Web Crypto API (`crypto.subtle`):
 *
 * - **Key management**: On first use, an AES-GCM 256-bit key is generated and
 *   exported as JWK to localStorage under `yole_crypto_key`. On subsequent uses
 *   the key is imported from localStorage.
 * - **Encryption**: Each `store()` call generates a random 12-byte IV, encrypts
 *   the value with AES-GCM, concatenates IV + ciphertext, Base64-encodes, and
 *   stores in localStorage.
 * - **Decryption**: Each `retrieve()` call Base64-decodes, splits the first 12
 *   bytes as IV and the rest as ciphertext, then decrypts with AES-GCM.
 *
 * ### Threat Model (Encrypted Mode)
 * - **Protected against:** Casual localStorage inspection, basic XSS data
 *   exfiltration (attacker gets ciphertext, not plaintext), offline analysis
 *   of exported localStorage dumps without the key
 * - **NOT protected against:** Sophisticated XSS that reads both the key and
 *   ciphertext from localStorage, browser extensions with full page access,
 *   physical device access with browser profile access
 *
 * ### Fallback (Non-Secure Context)
 *
 * When `crypto.subtle` is unavailable (plain HTTP, non-localhost), the
 * implementation falls back to XOR obfuscation with a static key. This provides
 * only data hiding, not cryptographic security. A warning is logged to the
 * browser console when fallback mode is active.
 *
 * @see SecureStorage
 * @see SecureStorageFactory
 */
class WebSecureStorage : SecureStorage {

    /**
     * Whether the Web Crypto API (`crypto.subtle`) is available.
     * Requires a secure context: HTTPS or localhost.
     * Note: No @Volatile needed — Wasm is single-threaded.
     */
    private var cryptoAvailable: Boolean = checkCryptoSubtleAvailable()

    /**
     * Whether the AES-GCM crypto key has been initialized (generated or loaded).
     * Once true, [encryptAesGcm] and [decryptAesGcm] can be called.
     */
    private var cryptoKeyReady: Boolean = false

    /**
     * Indicates whether true encryption (AES-GCM via Web Crypto API) is supported
     * in the current browser context.
     *
     * Returns `true` when `crypto.subtle` is available (secure context: HTTPS or
     * localhost). Returns `false` when only XOR obfuscation fallback is active.
     */
    val isEncryptionSupported: Boolean
        get() = cryptoAvailable

    /**
     * Returns the security level of the current storage implementation.
     *
     * - `"encrypted"` — AES-GCM via Web Crypto API (secure context)
     * - `"obfuscated"` — XOR obfuscation fallback (non-secure context)
     * - `"plaintext"` — Fallback if obfuscation fails
     */
    val securityLevel: String
        get() = if (cryptoAvailable) "encrypted" else "obfuscated"

    override suspend fun store(key: String, value: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            if (cryptoAvailable) {
                ensureCryptoKeyReady()
                val encrypted = encryptAesGcm(value)
                localStorage.setItem(getStorageKey(key), encrypted)
            } else {
                val obfuscated = obfuscateData(value)
                localStorage.setItem(getStorageKey(key), obfuscated)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun retrieve(key: String): Result<String?> = withContext(Dispatchers.Default) {
        try {
            val storedValue = localStorage.getItem(getStorageKey(key))
                ?: return@withContext Result.success(null)
            if (cryptoAvailable) {
                ensureCryptoKeyReady()
                val decrypted = decryptAesGcm(storedValue)
                Result.success(decrypted)
            } else {
                val deobfuscated = deobfuscateData(storedValue)
                Result.success(deobfuscated)
            }
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
            val testKey = "_secure_storage_test_"
            val testValue = "test_value_${Clock.System.now().toEpochMilliseconds()}"

            store(testKey, testValue)
            val retrieved = retrieve(testKey).getOrNull()
            delete(testKey)

            if (retrieved != testValue) {
                return@withContext Result.success(false)
            }

            // Returns true when AES-GCM encryption is active (secure context),
            // false when only XOR obfuscation fallback is in use.
            Result.success(cryptoAvailable)
        } catch (e: Exception) {
            Result.success(false)
        }
    }

    // ==================== AES-GCM Encryption ====================

    /**
     * Ensures the AES-GCM crypto key is ready for use.
     * Loads an existing key from localStorage or generates a new one.
     */
    private suspend fun ensureCryptoKeyReady() {
        if (cryptoKeyReady) return
        val existingJwk = localStorage.getItem(CRYPTO_KEY_STORAGE_KEY)
        if (existingJwk != null) {
            importCryptoKeyFromJwk(existingJwk)
        } else {
            generateAndStoreCryptoKey()
        }
        cryptoKeyReady = true
    }

    /**
     * Generates a new AES-GCM 256-bit key, exports it as JWK, and stores
     * the JWK string in localStorage.
     */
    private suspend fun generateAndStoreCryptoKey() {
        val jwk = jsGenerateAndExportKey()
        localStorage.setItem(CRYPTO_KEY_STORAGE_KEY, jwk)
    }

    /**
     * Imports an AES-GCM key from a JWK string stored in localStorage.
     */
    private suspend fun importCryptoKeyFromJwk(jwk: String) {
        jsImportKey(jwk)
    }

    /**
     * Encrypts a plaintext string using AES-GCM.
     *
     * Generates a random 12-byte IV, encrypts the plaintext, concatenates
     * IV + ciphertext, and returns the result as a Base64-encoded string.
     *
     * @param plaintext The string to encrypt
     * @return Base64-encoded IV + ciphertext
     */
    private suspend fun encryptAesGcm(plaintext: String): String {
        return jsEncrypt(plaintext)
    }

    /**
     * Decrypts a Base64-encoded IV + ciphertext string using AES-GCM.
     *
     * Base64-decodes the input, splits the first 12 bytes as IV and the
     * rest as ciphertext, and decrypts.
     *
     * @param encoded Base64-encoded IV + ciphertext
     * @return The decrypted plaintext string
     */
    private suspend fun decryptAesGcm(encoded: String): String {
        return jsDecrypt(encoded)
    }

    // ==================== XOR Obfuscation Fallback ====================

    private fun getStorageKey(key: String): String {
        return "$STORAGE_PREFIX$key"
    }

    private fun obfuscateData(data: String): String {
        // XOR obfuscation fallback for non-secure contexts.
        // WARNING: This is NOT secure encryption, just basic obfuscation to prevent
        // casual inspection of localStorage values.
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
        private const val CRYPTO_KEY_STORAGE_KEY = "yole_crypto_key"
        private val obfuscationKey = "Y0l3_N3tw0rk_S3cur3_K3y_2025"
    }
}

// ==================== External JS Functions ====================

// Base64 encoding/decoding (available in all modern browsers)
external fun btoa(data: String): String
external fun atob(data: String): String

// ==================== Web Crypto API JS Interop ====================
//
// The Web Crypto API is Promise-based and operates on ArrayBuffer/Uint8Array.
// These helper functions encapsulate the async JS operations and bridge them
// into Kotlin/Wasm via @JsFun annotations that define inline JavaScript.
//
// The crypto key is held in a globalThis variable (_yoleCryptoKey) so it
// persists across calls without re-importing from localStorage each time.
//
// Async JS functions return JsAny (which is a JS Promise at runtime). The
// awaitJsString() helper bridges these Promises into Kotlin coroutines using
// suspendCancellableCoroutine + .then()/.catch() callbacks.

/**
 * Checks whether `crypto.subtle` is available in the current browser context.
 * Returns `true` in secure contexts (HTTPS or localhost), `false` otherwise.
 * Logs a console warning when falling back to obfuscation mode.
 */
@JsFun("""
() => {
    try {
        if (typeof crypto !== 'undefined' && crypto.subtle) {
            return true;
        }
    } catch (e) {}
    console.warn('[Yole] crypto.subtle unavailable (non-secure context). Falling back to XOR obfuscation. Use HTTPS or localhost for AES-GCM encryption.');
    return false;
}
""")
private external fun checkCryptoSubtleAvailable(): Boolean

/**
 * Generates an AES-GCM 256-bit key, stores it in the globalThis variable,
 * exports it as JWK, and returns a Promise resolving to the JWK JSON string.
 */
@JsFun("""
async () => {
    const key = await crypto.subtle.generateKey(
        { name: 'AES-GCM', length: 256 },
        true,
        ['encrypt', 'decrypt']
    );
    globalThis._yoleCryptoKey = key;
    const jwk = await crypto.subtle.exportKey('jwk', key);
    return JSON.stringify(jwk);
}
""")
private external fun jsGenerateAndExportKeyImpl(): JsAny

/**
 * Imports an AES-GCM key from a JWK JSON string into the globalThis variable.
 * Returns a Promise resolving to an empty string on success.
 */
@JsFun("""
async (jwkStr) => {
    const jwk = JSON.parse(jwkStr);
    const key = await crypto.subtle.importKey(
        'jwk',
        jwk,
        { name: 'AES-GCM', length: 256 },
        true,
        ['encrypt', 'decrypt']
    );
    globalThis._yoleCryptoKey = key;
    return '';
}
""")
private external fun jsImportKeyImpl(jwk: JsString): JsAny

/**
 * Encrypts a plaintext string using AES-GCM with the globalThis key.
 * Returns a Promise resolving to Base64-encoded IV (12 bytes) + ciphertext.
 */
@JsFun("""
async (plaintext) => {
    const key = globalThis._yoleCryptoKey;
    if (!key) throw new Error('Crypto key not initialized');
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const encoder = new TextEncoder();
    const data = encoder.encode(plaintext);
    const ciphertext = await crypto.subtle.encrypt(
        { name: 'AES-GCM', iv: iv },
        key,
        data
    );
    const combined = new Uint8Array(iv.length + ciphertext.byteLength);
    combined.set(iv, 0);
    combined.set(new Uint8Array(ciphertext), iv.length);
    let binary = '';
    for (let i = 0; i < combined.length; i++) {
        binary += String.fromCharCode(combined[i]);
    }
    return btoa(binary);
}
""")
private external fun jsEncryptImpl(plaintext: JsString): JsAny

/**
 * Decrypts a Base64-encoded IV + ciphertext string using AES-GCM.
 * Returns a Promise resolving to the plaintext string.
 */
@JsFun("""
async (encoded) => {
    const key = globalThis._yoleCryptoKey;
    if (!key) throw new Error('Crypto key not initialized');
    const binary = atob(encoded);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
    }
    const iv = bytes.slice(0, 12);
    const ciphertext = bytes.slice(12);
    const decrypted = await crypto.subtle.decrypt(
        { name: 'AES-GCM', iv: iv },
        key,
        ciphertext
    );
    const decoder = new TextDecoder();
    return decoder.decode(decrypted);
}
""")
private external fun jsDecryptImpl(encoded: JsString): JsAny

/**
 * Bridges a JS Promise (typed as [JsAny]) to a Kotlin String via `.then()`/`.catch()`.
 *
 * The [onResolve] callback receives the resolved string value; [onReject] receives
 * the error message. This function is used by [awaitJsString] to connect JS Promises
 * to Kotlin's [suspendCancellableCoroutine].
 */
@JsFun("""
(promise, onResolve, onReject) => {
    promise.then(
        (value) => onResolve(String(value)),
        (error) => onReject(String(error && error.message ? error.message : error))
    );
}
""")
private external fun jsPromiseThen(
    promise: JsAny,
    onResolve: (JsString) -> Unit,
    onReject: (JsString) -> Unit
)

// ==================== Kotlin Suspend Bridges ====================
//
// These suspend functions bridge the JS Promise-based API into Kotlin coroutines.
// The awaitJsString() function uses suspendCancellableCoroutine with JS .then()/.catch()
// callbacks to resume the coroutine when the Promise settles.

/**
 * Awaits a JS Promise (typed as [JsAny]) and returns the resolved value as a Kotlin [String].
 * Throws an [Exception] if the Promise rejects.
 */
private suspend fun awaitJsString(promise: JsAny): String {
    return suspendCancellableCoroutine { continuation ->
        jsPromiseThen(
            promise,
            onResolve = { value: JsString ->
                continuation.resume(value.toString())
            },
            onReject = { error: JsString ->
                continuation.resumeWithException(RuntimeException(error.toString()))
            }
        )
    }
}

private suspend fun jsGenerateAndExportKey(): String {
    return awaitJsString(jsGenerateAndExportKeyImpl())
}

private suspend fun jsImportKey(jwk: String) {
    awaitJsString(jsImportKeyImpl(jwk.toJsString()))
}

private suspend fun jsEncrypt(plaintext: String): String {
    return awaitJsString(jsEncryptImpl(plaintext.toJsString()))
}

private suspend fun jsDecrypt(encoded: String): String {
    return awaitJsString(jsDecryptImpl(encoded.toJsString()))
}
