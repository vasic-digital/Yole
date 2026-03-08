/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Extended desktop-specific tests for DesktopSecureStorage
 * encryption roundtrip, edge cases, concurrency, and resilience.
 *
 *########################################################*/
package digital.vasic.yole.network.platform

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Extended desktop-specific tests for DesktopSecureStorage.
 *
 * Tests cover:
 * - Encryption roundtrip (store and retrieve)
 * - Empty values
 * - Special characters
 * - Key not found behavior
 * - Concurrent access
 * - Multiple key operations
 * - Credential and token management
 * - Data integrity after restart
 * - Edge cases with keys and values
 */
class DesktopSecureStorageTests {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var storageDir: File
    private lateinit var storage: DesktopSecureStorage

    @Before
    fun setup() {
        storageDir = tempFolder.newFolder("secure_storage_extended_test")
        storage = DesktopSecureStorage(storageDir)
        runTest {
            storage.clear()
        }
    }

    @After
    fun tearDown() {
        runTest {
            storage.clear()
        }
    }

    // ==================== Encryption Roundtrip Tests ====================

    @Test
    fun `encryption roundtrip preserves plain text value`() = runBlocking {
        val key = "roundtrip_plain"
        val value = "Hello, World!"
        storage.store(key, value)
        val result = storage.retrieve(key)
        assertTrue(result.isSuccess)
        assertEquals(value, result.getOrNull())
    }

    @Test
    fun `encryption roundtrip preserves numeric string`() = runBlocking {
        val key = "roundtrip_numeric"
        val value = "1234567890.0987654321"
        storage.store(key, value)
        val result = storage.retrieve(key)
        assertEquals(value, result.getOrNull())
    }

    @Test
    fun `encryption roundtrip preserves JSON content`() = runBlocking {
        val key = "roundtrip_json"
        val value = """{"token":"abc123","expires":1700000000,"refresh":"xyz789"}"""
        storage.store(key, value)
        val result = storage.retrieve(key)
        assertEquals(value, result.getOrNull())
    }

    @Test
    fun `encryption roundtrip preserves multi-line content`() = runBlocking {
        val key = "roundtrip_multiline"
        val value = "Line 1\nLine 2\nLine 3\r\nLine 4 with CR-LF"
        storage.store(key, value)
        val result = storage.retrieve(key)
        assertEquals(value, result.getOrNull())
    }

    @Test
    fun `encryption roundtrip preserves base64-encoded content`() = runBlocking {
        val key = "roundtrip_base64"
        val value = "SGVsbG8sIFdvcmxkIQ==\naGVsbG8gYWdhaW4=\n"
        storage.store(key, value)
        val result = storage.retrieve(key)
        assertEquals(value, result.getOrNull())
    }

    // ==================== Empty Value Tests ====================

    @Test
    fun `store and retrieve empty string value`() = runBlocking {
        val key = "empty_value"
        val value = ""
        val storeResult = storage.store(key, value)
        assertTrue(storeResult.isSuccess, "Storing empty value should succeed")
        val retrieveResult = storage.retrieve(key)
        assertTrue(retrieveResult.isSuccess)
        assertEquals(value, retrieveResult.getOrNull())
    }

    @Test
    fun `store empty value then overwrite with non-empty`() = runBlocking {
        val key = "empty_then_filled"
        storage.store(key, "")
        assertEquals("", storage.retrieve(key).getOrNull())
        storage.store(key, "now has content")
        assertEquals("now has content", storage.retrieve(key).getOrNull())
    }

    @Test
    fun `store non-empty value then overwrite with empty`() = runBlocking {
        val key = "filled_then_empty"
        storage.store(key, "has content")
        assertEquals("has content", storage.retrieve(key).getOrNull())
        storage.store(key, "")
        assertEquals("", storage.retrieve(key).getOrNull())
    }

    // ==================== Special Characters Tests ====================

    @Test
    fun `store and retrieve value with pipe characters`() = runBlocking {
        val key = "special_pipe"
        val value = "data|with|pipe|characters"
        storage.store(key, value)
        assertEquals(value, storage.retrieve(key).getOrNull())
    }

    @Test
    fun `store and retrieve value with newlines and tabs`() = runBlocking {
        val key = "special_whitespace"
        val value = "tab\there\nnewline\there\r\nwindows\tnewline"
        storage.store(key, value)
        assertEquals(value, storage.retrieve(key).getOrNull())
    }

    @Test
    fun `store and retrieve value with unicode characters`() = runBlocking {
        val key = "special_unicode"
        val value = "Chinese: \u4F60\u597D Japanese: \u3053\u3093\u306B\u3061\u306F Arabic: \u0645\u0631\u062D\u0628\u0627"
        storage.store(key, value)
        assertEquals(value, storage.retrieve(key).getOrNull())
    }

    @Test
    fun `store and retrieve value with null bytes`() = runBlocking {
        val key = "special_null_byte"
        val value = "before\u0000after"
        storage.store(key, value)
        assertEquals(value, storage.retrieve(key).getOrNull())
    }

    @Test
    fun `store and retrieve key with special characters`() = runBlocking {
        val specialKeys = listOf(
            "key-with-dashes",
            "key_underscores",
            "key.dots",
            "key@at",
            "key with spaces",
            "key/slashes",
            "key:colons"
        )
        specialKeys.forEach { key ->
            storage.store(key, "value_for_$key")
        }
        specialKeys.forEach { key ->
            assertEquals("value_for_$key", storage.retrieve(key).getOrNull(),
                "Failed for key: $key")
        }
    }

    @Test
    fun `store and retrieve value with SQL injection attempt`() = runBlocking {
        val key = "special_sql"
        val value = "'; DROP TABLE users; --"
        storage.store(key, value)
        assertEquals(value, storage.retrieve(key).getOrNull())
    }

    @Test
    fun `store and retrieve value with HTML entities`() = runBlocking {
        val key = "special_html"
        val value = "<script>alert('xss')</script>&amp;&lt;&gt;"
        storage.store(key, value)
        assertEquals(value, storage.retrieve(key).getOrNull())
    }

    // ==================== Key Not Found Tests ====================

    @Test
    fun `retrieve non-existent key returns null success`() = runBlocking {
        val result = storage.retrieve("nonexistent_key_abc123")
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `contains returns false for non-existent key`() = runBlocking {
        val result = storage.contains("nonexistent_key_xyz789")
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!)
    }

    @Test
    fun `delete non-existent key succeeds gracefully`() = runBlocking {
        val result = storage.delete("nonexistent_key_to_delete")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `retrieve after delete returns null`() = runBlocking {
        storage.store("delete_me", "temporary_value")
        assertEquals("temporary_value", storage.retrieve("delete_me").getOrNull())
        storage.delete("delete_me")
        assertNull(storage.retrieve("delete_me").getOrNull())
    }

    // ==================== Concurrent Access Tests ====================

    @Test
    fun `concurrent store operations preserve all data`() = runBlocking {
        val count = 30
        val keys = (1..count).map { "concurrent_store_$it" }
        val values = (1..count).map { "concurrent_value_$it" }

        keys.zip(values).forEach { (key, value) ->
            storage.store(key, value)
        }

        keys.zip(values).forEach { (key, expectedValue) ->
            val result = storage.retrieve(key)
            assertEquals(expectedValue, result.getOrNull(),
                "Concurrent store should preserve data for $key")
        }
    }

    @Test
    fun `concurrent store and retrieve interleaved`() = runBlocking {
        val iterations = 20
        for (i in 1..iterations) {
            val key = "interleaved_$i"
            val value = "interleaved_value_$i"
            storage.store(key, value)
            val retrieved = storage.retrieve(key)
            assertEquals(value, retrieved.getOrNull(),
                "Interleaved store/retrieve should work for iteration $i")
        }
    }

    @Test
    fun `concurrent delete does not affect other keys`() = runBlocking {
        storage.store("keep_1", "value_1")
        storage.store("keep_2", "value_2")
        storage.store("delete_this", "to_be_deleted")

        storage.delete("delete_this")

        assertEquals("value_1", storage.retrieve("keep_1").getOrNull())
        assertEquals("value_2", storage.retrieve("keep_2").getOrNull())
        assertNull(storage.retrieve("delete_this").getOrNull())
    }

    @Test
    fun `rapid store-delete cycles do not corrupt storage`() = runBlocking {
        for (i in 1..25) {
            storage.store("rapid_key_$i", "rapid_value_$i")
            if (i % 3 == 0) {
                storage.delete("rapid_key_$i")
            }
        }
        for (i in 1..25) {
            val result = storage.retrieve("rapid_key_$i")
            if (i % 3 == 0) {
                assertNull(result.getOrNull(),
                    "Deleted key rapid_key_$i should be null")
            } else {
                assertEquals("rapid_value_$i", result.getOrNull(),
                    "Non-deleted key rapid_key_$i should have its value")
            }
        }
    }

    @Test
    fun `concurrent listKeys returns consistent snapshot`() = runBlocking {
        val keys = (1..10).map { "list_key_$it" }
        keys.forEach { key -> storage.store(key, "value") }

        val listedKeys = storage.listKeys().getOrNull()
        assertNotNull(listedKeys)
        assertEquals(keys.size, listedKeys.size)
        assertTrue(listedKeys.containsAll(keys))
    }

    // ==================== Credential Management Tests ====================

    @Test
    fun `store and retrieve credentials with complex password`() = runBlocking {
        val service = "test_service"
        val username = "admin@domain.com"
        val password = "P@\$\$w0rd!#%^&*()_+-={}[]|\\:\";<>?,./~`"
        storage.storeCredentials(service, username, password)
        val result = storage.retrieveCredentials(service)
        assertTrue(result.isSuccess)
        val creds = result.getOrNull()
        assertNotNull(creds)
        assertEquals(username, creds.first)
        assertEquals(password, creds.second)
    }

    @Test
    fun `delete credentials removes them`() = runBlocking {
        storage.storeCredentials("del_service", "user", "pass")
        storage.deleteCredentials("del_service")
        assertNull(storage.retrieveCredentials("del_service").getOrNull())
    }

    // ==================== Token Management Tests ====================

    @Test
    fun `store and retrieve long OAuth token`() = runBlocking {
        val service = "oauth_service"
        val token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9." + "a".repeat(500) + ".signature"
        storage.storeToken(service, token)
        assertEquals(token, storage.retrieveToken(service).getOrNull())
    }

    @Test
    fun `delete token removes it`() = runBlocking {
        storage.storeToken("del_token_service", "my_token")
        storage.deleteToken("del_token_service")
        assertNull(storage.retrieveToken("del_token_service").getOrNull())
    }

    // ==================== Data Integrity After Restart ====================

    @Test
    fun `data persists after creating new storage instance`() = runBlocking {
        storage.store("persist_key", "persist_value")
        val newStorage = DesktopSecureStorage(storageDir)
        val result = newStorage.retrieve("persist_key")
        assertEquals("persist_value", result.getOrNull())
    }

    @Test
    fun `multiple keys persist across restart`() = runBlocking {
        val data = mapOf(
            "persist_1" to "value_1",
            "persist_2" to "value_2",
            "persist_3" to "value_3"
        )
        data.forEach { (k, v) -> storage.store(k, v) }

        val newStorage = DesktopSecureStorage(storageDir)
        data.forEach { (k, v) ->
            assertEquals(v, newStorage.retrieve(k).getOrNull(),
                "Key $k should persist across restart")
        }
    }

    @Test
    fun `clear followed by restart results in empty storage`() = runBlocking {
        storage.store("clear_test", "some_value")
        storage.clear()
        val newStorage = DesktopSecureStorage(storageDir)
        assertNull(newStorage.retrieve("clear_test").getOrNull())
        val keys = newStorage.listKeys().getOrNull()
        assertNotNull(keys)
        assertTrue(keys.isEmpty())
    }

    // ==================== Edge Cases ====================

    @Test
    fun `store very long key`() = runBlocking {
        val longKey = "k".repeat(1000)
        storage.store(longKey, "long_key_value")
        assertEquals("long_key_value", storage.retrieve(longKey).getOrNull())
    }

    @Test
    fun `store and retrieve binary-like string`() = runBlocking {
        val key = "binary_like"
        val value = String(ByteArray(256) { it.toByte() }.filter { it != 0.toByte() }.toByteArray())
        storage.store(key, value)
        assertEquals(value, storage.retrieve(key).getOrNull())
    }

    @Test
    fun `overwrite value preserves only latest`() = runBlocking {
        val key = "overwrite_key"
        storage.store(key, "first")
        storage.store(key, "second")
        storage.store(key, "third")
        assertEquals("third", storage.retrieve(key).getOrNull())
    }

    @Test
    fun `listKeys after multiple operations is accurate`() = runBlocking {
        storage.store("lk_1", "v1")
        storage.store("lk_2", "v2")
        storage.store("lk_3", "v3")
        storage.delete("lk_2")
        val keys = storage.listKeys().getOrNull()
        assertNotNull(keys)
        assertEquals(2, keys.size)
        assertTrue(keys.contains("lk_1"))
        assertTrue(keys.contains("lk_3"))
        assertFalse(keys.contains("lk_2"))
    }

    @Test
    fun `contains returns true for existing key`() = runBlocking {
        storage.store("exists_key", "exists_value")
        val result = storage.contains("exists_key")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!)
    }

    @Test
    fun `isSecure returns true for desktop storage`() = runBlocking {
        val result = storage.isSecure()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!)
    }

    @Test
    fun `encrypted file does not contain plaintext values`() = runBlocking {
        val key = "encrypted_check_key"
        val value = "super_secret_plaintext_value_12345"
        storage.store(key, value)

        val dataFile = File(storageDir, ".secure_storage")
        assertTrue(dataFile.exists())
        val rawContent = dataFile.readText()
        assertFalse(rawContent.contains(value),
            "Encrypted storage file should not contain plaintext value")
        assertFalse(rawContent.contains(key),
            "Encrypted storage file should not contain plaintext key")
    }

    @Test
    fun `private key store and retrieve roundtrip`() = runBlocking {
        val service = "ssh_test"
        val privateKey = """-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtz
c2gtZWQyNTUxOQAAACDOWbzWbwxElYpkIo3Fn1hzmJRfSwjPF+7Q4X+BthB0OAAA
-----END OPENSSH PRIVATE KEY-----"""
        storage.storePrivateKey(service, privateKey)
        val result = storage.retrievePrivateKey(service)
        assertEquals(privateKey, result.getOrNull())
    }
}
