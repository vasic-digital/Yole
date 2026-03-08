/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for SecureStorage interface and common functionality
 *
 *########################################################*/
package digital.vasic.yole.network.platform

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Unit tests for SecureStorage interface.
 *
 * Tests cover:
 * - Basic CRUD operations (store, retrieve, delete)
 * - Credential management (username/password)
 * - Token management
 * - Private key management
 * - Key listing and clearing
 * - Security validation
 * - Error handling
 */
abstract class SecureStorageTest {

    abstract suspend fun createStorage(): SecureStorage

    // ==================== Basic Operations Tests ====================

    @Test
    fun `should store and retrieve basic values`() = runBlocking {
        val storage = createStorage()
        val key = "test_key"
        val value = "test_value_123"

        val storeResult = storage.store(key, value)
        assertTrue(storeResult.isSuccess, "Store operation should succeed")

        val retrieveResult = storage.retrieve(key)
        assertTrue(retrieveResult.isSuccess, "Retrieve operation should succeed")
        assertEquals(value, retrieveResult.getOrNull())
    }

    @Test
    fun `should return null for non-existent keys`() = runBlocking {
        val storage = createStorage()
        val key = "non_existent_key"

        val result = storage.retrieve(key)
        assertTrue(result.isSuccess, "Retrieve should succeed even for non-existent key")
        assertNull(result.getOrNull())
    }

    @Test
    fun `should update existing values`() = runBlocking {
        val storage = createStorage()
        val key = "update_test_key"
        val initialValue = "initial_value"
        val updatedValue = "updated_value"

        storage.store(key, initialValue)
        storage.store(key, updatedValue)

        val result = storage.retrieve(key)
        assertEquals(updatedValue, result.getOrNull())
    }

    @Test
    fun `should delete existing values`() = runBlocking {
        val storage = createStorage()
        val key = "delete_test_key"
        val value = "delete_test_value"

        storage.store(key, value)
        val deleteResult = storage.delete(key)
        assertTrue(deleteResult.isSuccess, "Delete operation should succeed")

        val retrieveResult = storage.retrieve(key)
        assertNull(retrieveResult.getOrNull())
    }

    @Test
    fun `should handle delete of non-existent key gracefully`() = runBlocking {
        val storage = createStorage()
        val key = "non_existent_delete_key"

        val result = storage.delete(key)
        assertTrue(result.isSuccess, "Delete should succeed even for non-existent key")
    }

    @Test
    fun `should check if key exists`() = runBlocking {
        val storage = createStorage()
        val existingKey = "existing_key"
        val nonExistingKey = "non_existing_key"
        val value = "test_value"

        storage.store(existingKey, value)

        val existingResult = storage.contains(existingKey)
        assertTrue(existingResult.isSuccess, "Contains should succeed for existing key")
        assertTrue(existingResult.getOrNull() ?: false, "Should contain existing key")

        val nonExistingResult = storage.contains(nonExistingKey)
        assertTrue(nonExistingResult.isSuccess, "Contains should succeed for non-existing key")
        assertFalse(nonExistingResult.getOrNull() ?: true, "Should not contain non-existing key")
    }

    @Test
    fun `should list all keys`() = runBlocking {
        val storage = createStorage()
        val keys = listOf("key1", "key2", "key3")
        val value = "test_value"

        keys.forEach { key ->
            storage.store(key, value)
        }

        val listResult = storage.listKeys()
        assertTrue(listResult.isSuccess, "List keys should succeed")
        val storedKeys = listResult.getOrNull()
        assertNotNull(storedKeys)
        assertTrue(storedKeys.containsAll(keys) && keys.containsAll(storedKeys), "Keys should match exactly")
    }

    @Test
    fun `should clear all stored values`() = runBlocking {
        val storage = createStorage()
        val keys = listOf("key1", "key2", "key3")
        val value = "test_value"

        keys.forEach { key ->
            storage.store(key, value)
        }

        val clearResult = storage.clear()
        assertTrue(clearResult.isSuccess, "Clear operation should succeed")

        val listResult = storage.listKeys()
        assertTrue(listResult.getOrNull()?.isEmpty() ?: true, "Should have no keys after clear")
    }

    // ==================== Credential Management Tests ====================

    @Test
    fun `should store and retrieve credentials`() = runBlocking {
        val storage = createStorage()
        val service = "webdav"
        val username = "testuser"
        val password = "testpass123"

        val storeResult = storage.storeCredentials(service, username, password)
        assertTrue(storeResult.isSuccess, "Store credentials should succeed")

        val retrieveResult = storage.retrieveCredentials(service)
        assertTrue(retrieveResult.isSuccess, "Retrieve credentials should succeed")
        val credentials = retrieveResult.getOrNull()
        assertNotNull(credentials)
        assertEquals(username, credentials.first)
        assertEquals(password, credentials.second)
    }

    @Test
    fun `should return null for non-existent credentials`() = runBlocking {
        val storage = createStorage()
        val service = "non_existent_service"

        val result = storage.retrieveCredentials(service)
        assertTrue(result.isSuccess, "Retrieve should succeed even for non-existent service")
        assertNull(result.getOrNull())
    }

    @Test
    fun `should delete credentials`() = runBlocking {
        val storage = createStorage()
        val service = "sftp"
        val username = "testuser"
        val password = "testpass123"

        storage.storeCredentials(service, username, password)
        val deleteResult = storage.deleteCredentials(service)
        assertTrue(deleteResult.isSuccess, "Delete credentials should succeed")

        val retrieveResult = storage.retrieveCredentials(service)
        assertNull(retrieveResult.getOrNull())
    }

    @Test
    fun `should handle credentials with special characters`() = runBlocking {
        val storage = createStorage()
        val service = "ftp"
        val username = "user@domain.com"
        val password = "p@ssw0rd!#$%^&*()"

        storage.storeCredentials(service, username, password)
        val result = storage.retrieveCredentials(service)
        
        val credentials = result.getOrNull()
        assertNotNull(credentials)
        assertEquals(username, credentials.first)
        assertEquals(password, credentials.second)
    }

    @Test
    fun `should handle credentials with colons in username or password`() = runBlocking {
        val storage = createStorage()
        val service = "webdav"
        val username = "domain:user"
        val password = "pass:word:with:colons"

        storage.storeCredentials(service, username, password)
        val result = storage.retrieveCredentials(service)
        
        val credentials = result.getOrNull()
        assertNotNull(credentials)
        assertEquals(username, credentials.first)
        assertEquals(password, credentials.second)
    }

    // ==================== Token Management Tests ====================

    @Test
    fun `should store and retrieve tokens`() = runBlocking {
        val storage = createStorage()
        val service = "dropbox"
        val token = "sl.test_token_abc123xyz"

        val storeResult = storage.storeToken(service, token)
        assertTrue(storeResult.isSuccess, "Store token should succeed")

        val retrieveResult = storage.retrieveToken(service)
        assertTrue(retrieveResult.isSuccess, "Retrieve token should succeed")
        assertEquals(token, retrieveResult.getOrNull())
    }

    @Test
    fun `should return null for non-existent tokens`() = runBlocking {
        val storage = createStorage()
        val service = "non_existent_service"

        val result = storage.retrieveToken(service)
        assertTrue(result.isSuccess, "Retrieve should succeed even for non-existent service")
        assertNull(result.getOrNull())
    }

    @Test
    fun `should delete tokens`() = runBlocking {
        val storage = createStorage()
        val service = "googledrive"
        val token = "test_token_123"

        storage.storeToken(service, token)
        val deleteResult = storage.deleteToken(service)
        assertTrue(deleteResult.isSuccess, "Delete token should succeed")

        val retrieveResult = storage.retrieveToken(service)
        assertNull(retrieveResult.getOrNull())
    }

    @Test
    fun `should handle long tokens`() = runBlocking {
        val storage = createStorage()
        val service = "onedrive"
        val token = "a".repeat(2048) // 2KB token

        storage.storeToken(service, token)
        val result = storage.retrieveToken(service)
        
        assertEquals(token, result.getOrNull())
    }

    // ==================== Private Key Management Tests ====================

    @Test
    fun `should store and retrieve private keys`() = runBlocking {
        val storage = createStorage()
        val service = "sftp"
        val privateKey = """
            -----BEGIN RSA PRIVATE KEY-----
            MIIEpAIBAAKCAQEA2nP8+YXgFYZx5hR+1X9mOLfJqZJQHbPDuPg0+NXoON1qYF1P
            8WkB0yQ5gJ9jJ6K5Y8V3nF7P9L2M4Q6R8T0U1V2W3X4Y5Z6A7B8C9D0E1F2G3H4I5
            -----END RSA PRIVATE KEY-----
        """.trimIndent()

        val storeResult = storage.storePrivateKey(service, privateKey)
        assertTrue(storeResult.isSuccess, "Store private key should succeed")

        val retrieveResult = storage.retrievePrivateKey(service)
        assertTrue(retrieveResult.isSuccess, "Retrieve private key should succeed")
        assertEquals(privateKey, retrieveResult.getOrNull())
    }

    @Test
    fun `should return null for non-existent private keys`() = runBlocking {
        val storage = createStorage()
        val service = "non_existent_service"

        val result = storage.retrievePrivateKey(service)
        assertTrue(result.isSuccess, "Retrieve should succeed even for non-existent service")
        assertNull(result.getOrNull())
    }

    @Test
    fun `should delete private keys`() = runBlocking {
        val storage = createStorage()
        val service = "git"
        val privateKey = "-----BEGIN PRIVATE KEY-----\ntest key data\n-----END PRIVATE KEY-----"

        storage.storePrivateKey(service, privateKey)
        val deleteResult = storage.deletePrivateKey(service)
        assertTrue(deleteResult.isSuccess, "Delete private key should succeed")

        val retrieveResult = storage.retrievePrivateKey(service)
        assertNull(retrieveResult.getOrNull())
    }

    // ==================== Security Validation Tests ====================

    @Test
    fun `should validate security status`() = runBlocking {
        val storage = createStorage()

        val result = storage.isSecure()
        assertTrue(result.isSuccess, "Security check should succeed")
        val isSecure = result.getOrNull()
        assertNotNull(isSecure, "Security status should not be null")
        // Note: Actual security status depends on platform implementation
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `should handle empty keys gracefully`() = runBlocking {
        val storage = createStorage()
        val value = "test_value"

        val result = storage.store("", value)
        assertTrue(result.isSuccess, "Store with empty key should succeed")
    }

    @Test
    fun `should handle empty values`() = runBlocking {
        val storage = createStorage()
        val key = "empty_value_key"

        val storeResult = storage.store(key, "")
        assertTrue(storeResult.isSuccess, "Store empty value should succeed")

        val retrieveResult = storage.retrieve(key)
        assertEquals("", retrieveResult.getOrNull())
    }

    @Test
    fun `should handle very large values`() = runBlocking {
        val storage = createStorage()
        val key = "large_value_key"
        val largeValue = "x".repeat(1024 * 1024) // 1MB value

        val storeResult = storage.store(key, largeValue)
        assertTrue(storeResult.isSuccess, "Store large value should succeed")

        val retrieveResult = storage.retrieve(key)
        assertEquals(largeValue, retrieveResult.getOrNull())
    }

    @Test
    fun `should handle special characters in keys`() = runBlocking {
        val storage = createStorage()
        val specialKeys = listOf(
            "key-with-dashes",
            "key_with_underscores",
            "key.with.dots",
            "key/with/slashes",
            "key:with:colons",
            "key with spaces",
            "key@with@symbols",
            "key#with#hash",
            "unicode_key_你好",
            "emoji_key_🚀"
        )
        val value = "test_value"

        specialKeys.forEach { key ->
            storage.store(key, value)
            val result = storage.retrieve(key)
            assertEquals(value, result.getOrNull())
        }
    }

    @Test
    fun `should handle unicode content`() = runBlocking {
        val storage = createStorage()
        val key = "unicode_key"
        val unicodeValue = """
            ASCII: Hello World
            Latin: Café résumé naïve
            Cyrillic: Привет мир
            Greek: Γεια σας κόσμε
            Chinese: 你好世界
            Japanese: こんにちは世界
            Arabic: مرحبا بالعالم
            Hebrew: שלום עולם
            Emoji: 🌍 🚀 💻 🔐
        """.trimIndent()

        storage.store(key, unicodeValue)
        val result = storage.retrieve(key)
        assertEquals(unicodeValue, result.getOrNull())
    }
}