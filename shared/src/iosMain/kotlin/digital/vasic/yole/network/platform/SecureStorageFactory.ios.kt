/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS implementation of SecureStorageFactory
 * Uses iOS Keychain Services for secure credential storage
 *
 *########################################################*/
package digital.vasic.yole.network.platform

import platform.Foundation.*
import platform.Security.*
import kotlinx.cinterop.*

/**
 * iOS implementation of SecureStorageFactory.
 * Uses iOS Keychain Services for secure credential storage.
 */
actual object SecureStorageFactory {

    /**
     * Create a new iOS secure storage instance backed by Keychain.
     */
    actual suspend fun create(): Result<SecureStorage> {
        return try {
            val secureStorage = IosKeychainSecureStorage()
            Result.success(secureStorage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if secure storage is available on iOS.
     * Keychain is always available on iOS.
     */
    actual suspend fun isAvailable(): Boolean = true
}

/**
 * iOS Keychain-backed SecureStorage implementation.
 */
private class IosKeychainSecureStorage : SecureStorage {

    private val serviceName = "digital.vasic.yole"

    override suspend fun store(key: String, value: String): Result<Unit> {
        return try {
            // Delete existing entry first
            delete(key)

            val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding)
                ?: return Result.failure(Exception("Failed to encode value"))

            val query = mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to serviceName,
                kSecAttrAccount to key,
                kSecValueData to data
            )

            @Suppress("UNCHECKED_CAST")
            val status = SecItemAdd(query as CFDictionaryRef, null)
            if (status == errSecSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Keychain store failed with status: $status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun retrieve(key: String): Result<String?> {
        return try {
            val query = mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to serviceName,
                kSecAttrAccount to key,
                kSecReturnData to true,
                kSecMatchLimit to kSecMatchLimitOne
            )

            memScoped {
                val result = alloc<ObjCObjectVar<Any?>>()
                @Suppress("UNCHECKED_CAST")
                val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)

                if (status == errSecSuccess) {
                    val data = result.value as? NSData
                    val value = data?.let {
                        NSString.create(data = it, encoding = NSUTF8StringEncoding) as? String
                    }
                    Result.success(value)
                } else if (status == errSecItemNotFound) {
                    Result.success(null)
                } else {
                    Result.failure(Exception("Keychain retrieve failed with status: $status"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(key: String): Result<Unit> {
        return try {
            val query = mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to serviceName,
                kSecAttrAccount to key
            )

            @Suppress("UNCHECKED_CAST")
            val status = SecItemDelete(query as CFDictionaryRef)
            if (status == errSecSuccess || status == errSecItemNotFound) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Keychain delete failed with status: $status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exists(key: String): Boolean {
        val result = retrieve(key)
        return result.isSuccess && result.getOrNull() != null
    }

    override suspend fun clear(): Result<Unit> {
        return try {
            val query = mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to serviceName
            )

            @Suppress("UNCHECKED_CAST")
            val status = SecItemDelete(query as CFDictionaryRef)
            if (status == errSecSuccess || status == errSecItemNotFound) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Keychain clear failed with status: $status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllKeys(): Result<List<String>> {
        return try {
            val query = mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to serviceName,
                kSecReturnAttributes to true,
                kSecMatchLimit to kSecMatchLimitAll
            )

            memScoped {
                val result = alloc<ObjCObjectVar<Any?>>()
                @Suppress("UNCHECKED_CAST")
                val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)

                if (status == errSecSuccess) {
                    val items = result.value as? List<Map<Any?, Any?>> ?: emptyList()
                    val keys = items.mapNotNull { it[kSecAttrAccount] as? String }
                    Result.success(keys)
                } else if (status == errSecItemNotFound) {
                    Result.success(emptyList())
                } else {
                    Result.failure(Exception("Keychain getAllKeys failed with status: $status"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
