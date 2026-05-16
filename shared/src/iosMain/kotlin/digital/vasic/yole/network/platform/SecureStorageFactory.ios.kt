/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS implementation of SecureStorageFactory
 * Uses iOS Keychain Services for secure credential storage.
 *
 *########################################################*/
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package digital.vasic.yole.network.platform

import platform.Foundation.*
import platform.Security.*
import platform.CoreFoundation.*
import kotlinx.cinterop.*

/**
 * iOS implementation of SecureStorageFactory.
 * Uses iOS Keychain Services for secure credential storage.
 */
actual object SecureStorageFactory {

    actual suspend fun create(): Result<SecureStorage> = runCatching {
        IosKeychainSecureStorage()
    }

    actual suspend fun isAvailable(): Boolean = true
}

/**
 * iOS Keychain-backed SecureStorage implementation.
 *
 * Uses Keychain Services API (SecItemAdd / SecItemCopyMatching /
 * SecItemDelete) via K/N interop. The Keychain query dictionaries are
 * built as Kotlin `Map<Any?, Any?>` and bridged to `CFDictionaryRef`
 * via the `@Suppress("UNCHECKED_CAST")` convention required by K/N.
 */
@Suppress("UNCHECKED_CAST")
private class IosKeychainSecureStorage : SecureStorage {

    private val serviceName = "digital.vasic.yole"

    override suspend fun store(key: String, value: String): Result<Unit> = runCatching {
        // Remove any stale entry first
        delete(key)

        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding)
            ?: error("Failed to encode value to NSData")

        val query = cfDictOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to (serviceName as NSString),
            kSecAttrAccount to (key as NSString),
            kSecValueData to data
        )

        val status = SecItemAdd(query, null)
        // K/N manages CF object lifetimes via ARC — no manual release needed
        check(status == errSecSuccess) { "Keychain store failed: $status" }
    }

    override suspend fun retrieve(key: String): Result<String?> = runCatching {
        val query = cfDictOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to (serviceName as NSString),
            kSecAttrAccount to (key as NSString),
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne
        )

        val result: String? = memScoped {
            val resultPtr = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, resultPtr.ptr)
            // K/N manages CF object lifetimes via ARC — no manual release needed
            when (status) {
                errSecSuccess -> {
                    val data = resultPtr.value as? NSData
                    data?.let {
                        NSString.create(data = it, encoding = NSUTF8StringEncoding) as? String
                    }
                }
                errSecItemNotFound -> null
                else -> error("Keychain retrieve failed: $status")
            }
        }
        result
    }

    override suspend fun delete(key: String): Result<Unit> = runCatching {
        val query = cfDictOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to (serviceName as NSString),
            kSecAttrAccount to (key as NSString)
        )
        val status = SecItemDelete(query)
        // K/N manages CF object lifetimes via ARC — no manual release needed
        check(status == errSecSuccess || status == errSecItemNotFound) {
            "Keychain delete failed: $status"
        }
    }

    override suspend fun contains(key: String): Result<Boolean> = runCatching {
        val query = cfDictOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to (serviceName as NSString),
            kSecAttrAccount to (key as NSString),
            kSecReturnData to kCFBooleanFalse
        )
        val status = SecItemCopyMatching(query, null)
        // K/N manages CF object lifetimes via ARC — no manual release needed
        status == errSecSuccess
    }

    override suspend fun clear(): Result<Unit> = runCatching {
        val query = cfDictOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to (serviceName as NSString)
        )
        val status = SecItemDelete(query)
        // K/N manages CF object lifetimes via ARC — no manual release needed
        check(status == errSecSuccess || status == errSecItemNotFound) {
            "Keychain clear failed: $status"
        }
    }

    override suspend fun isSecure(): Result<Boolean> = Result.success(true)

    override suspend fun listKeys(): Result<List<String>> = runCatching {
        val query = cfDictOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to (serviceName as NSString),
            kSecReturnAttributes to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitAll
        )

        val keys: List<String> = memScoped {
            val resultPtr = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, resultPtr.ptr)
            // K/N manages CF object lifetimes via ARC — no manual release needed
            when (status) {
                errSecSuccess -> {
                    @Suppress("UNCHECKED_CAST")
                    val items = resultPtr.value as? List<Map<Any?, Any?>> ?: emptyList()
                    items.mapNotNull { it[kSecAttrAccount] as? String }
                }
                errSecItemNotFound -> emptyList()
                else -> error("Keychain getAllKeys failed: $status")
            }
        }
        keys
    }

    /** Build a CFDictionaryRef from a vararg list of key-value pairs. */
    private fun cfDictOf(vararg pairs: Pair<Any?, Any?>): CFDictionaryRef {
        val map = mapOf(*pairs) as Map<Any?, Any?>
        return map as CFDictionaryRef
    }
}
