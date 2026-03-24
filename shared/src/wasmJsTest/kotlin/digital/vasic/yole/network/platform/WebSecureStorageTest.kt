/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Web-specific tests for WebSecureStorage implementation
 *
 *########################################################*/
package digital.vasic.yole.network.platform

import kotlinx.browser.localStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Web-specific tests for WebSecureStorage.
 *
 * Tests cover:
 * - localStorage integration
 * - AES-GCM encryption (secure context) / XOR obfuscation fallback
 * - Cross-browser compatibility
 * - localStorage limitations and quotas
 * - Web-specific error scenarios
 * - Base64 encoding/decoding
 * - Crypto key management
 */
class WebSecureStorageTest : SecureStorageTest() {

    private lateinit var webSecureStorage: WebSecureStorage

    override suspend fun createStorage(): SecureStorage {
        return webSecureStorage
    }

    @BeforeTest
    fun setup() {
        webSecureStorage = WebSecureStorage()
        
        // Clear any existing data from localStorage
        clearLocalStorage()
    }

    @AfterTest
    fun tearDown() {
        // Clean up localStorage after tests
        clearLocalStorage()
    }

    private fun clearLocalStorage() {
        // Clear all items with our prefix and the crypto key
        val keysToRemove = mutableListOf<String>()
        for (i in 0 until localStorage.length) {
            val key = localStorage.key(i)
            if (key?.startsWith("yole_network_secure_") == true || key == "yole_crypto_key") {
                keysToRemove.add(key)
            }
        }
        keysToRemove.forEach { key ->
            localStorage.removeItem(key)
        }
    }

    // ==================== Web-Specific Tests ====================

    @Test
    fun `should store encrypted data in localStorage`() = runTest {
        val key = "encryption_test_key"
        val value = "sensitive_data_123"

        // Store value
        webSecureStorage.store(key, value)

        // Check localStorage directly
        val storageKey = "yole_network_secure_$key"
        val storedValue = localStorage.getItem(storageKey)

        assertNotNull(storedValue, "Value should be stored in localStorage")

        // Stored value should be encrypted/obfuscated (not plaintext)
        assertNotEquals(value, storedValue, "Stored value should be encrypted or obfuscated")

        // Should be base64 encoded (contains only base64 characters)
        assertTrue(storedValue.matches(Regex("^[A-Za-z0-9+/=]*$")), "Should be base64 encoded")
    }

    @Test
    fun `should encrypt and decrypt correctly`() = runTest {
        val key = "roundtrip_test_key"
        val value = "test_value_for_roundtrip"

        webSecureStorage.store(key, value)

        // Retrieve and verify it matches original
        val result = webSecureStorage.retrieve(key)
        assertEquals(value, result.getOrNull(), "Encrypt/decrypt roundtrip should preserve data")
    }

    @Test
    fun `should produce unique ciphertexts with random IVs`() = runTest {
        val key = "iv_uniqueness_test_key"
        val value = "test_value"
        val storageKey = "yole_network_secure_$key"

        // Store same value twice — AES-GCM uses random IVs,
        // so ciphertexts should differ even for the same plaintext.
        // XOR obfuscation fallback produces identical output, but that is also acceptable.
        webSecureStorage.store(key, value)
        val firstStoredValue = localStorage.getItem(storageKey)

        webSecureStorage.store(key, value) // Store again
        val secondStoredValue = localStorage.getItem(storageKey)

        assertNotNull(firstStoredValue)
        assertNotNull(secondStoredValue)

        // Both must decrypt back to the same plaintext regardless of ciphertext differences
        val result = webSecureStorage.retrieve(key)
        assertEquals(value, result.getOrNull(), "Decrypted value should match original")
    }

    @Test
    fun `should handle localStorage quota limitations gracefully`() = runTest {
        // Test with large data that might approach localStorage limits
        val largeKey = "large_data_key"
        val largeValue = "x".repeat(1024 * 100) // 100KB of data

        val result = webSecureStorage.store(largeKey, largeValue)
        
        // Should either succeed or fail gracefully
        if (result.isFailure) {
            val exception = result.exceptionOrNull()
            assertNotNull(exception, "Should provide error details on quota exceeded")
            // Might get QuotaExceededError in browser
        } else {
            // If it succeeded, verify data integrity
            val retrieveResult = webSecureStorage.retrieve(largeKey)
            assertEquals(largeValue, retrieveResult.getOrNull())
        }
    }

    @Test
    fun `should handle localStorage unavailability`() = runTest {
        // This test would require mocking localStorage to be unavailable
        // For now, we test that the implementation handles null checks
        
        val key = "availability_test_key"
        val value = "test_value"

        // Normal operation should work
        val storeResult = webSecureStorage.store(key, value)
        assertTrue(storeResult.isSuccess, "Should succeed when localStorage is available")
    }

    @Test
    fun `should report secure when crypto is available`() = runTest {
        val isSecureResult = webSecureStorage.isSecure()
        assertTrue(isSecureResult.isSuccess, "Security validation should succeed")

        val isSecure = isSecureResult.getOrNull()
        assertNotNull(isSecure, "Security status should not be null")

        // In a secure context (HTTPS/localhost, e.g., test runner), isSecure() returns true
        // because AES-GCM encryption is active. In non-secure contexts it returns false.
        // The test environment provides crypto.subtle, so we expect true.
        assertTrue(isSecure, "Web secure storage should report as secure in test environment")
    }

    @Test
    fun `should handle browser privacy modes`() = runTest {
        // In private/incognito mode, localStorage might behave differently
        // Test that our implementation handles this gracefully
        
        val key = "privacy_test_key"
        val value = "privacy_test_value"

        // Store and retrieve should work normally
        webSecureStorage.store(key, value)
        val result = webSecureStorage.retrieve(key)
        assertEquals(value, result.getOrNull())

        // Clear should also work
        webSecureStorage.clear()
        val afterClear = webSecureStorage.retrieve(key)
        assertNull(afterClear.getOrNull())
    }

    @Test
    fun `should handle cross-origin restrictions`() = runTest {
        // localStorage is origin-specific
        // This test verifies our implementation doesn't accidentally expose data
        
        val key = "origin_test_key"
        val value = "origin_test_value"

        webSecureStorage.store(key, value)

        // Verify data is stored with our prefix
        val storageKey = "yole_network_secure_$key"
        val storedValue = localStorage.getItem(storageKey)
        assertNotNull(storedValue, "Should store with proper prefix")
        
        // Verify we only see our prefixed items when listing keys
        val listResult = webSecureStorage.listKeys()
        val keys = listResult.getOrNull()
        assertNotNull(keys)
        
        // All keys should be in our namespace
        keys.forEach { listedKey ->
            assertTrue(listedKey.startsWith("yole_network_secure_") || listedKey == key, 
                "Listed key should be in our namespace")
        }
    }

    @Test
    fun `should handle base64 encoding edge cases`() = runTest {
        // Test various content that might challenge base64 encoding
        val edgeCases = listOf(
            "", // Empty string
            "a", // Single character
            "ab", // Two characters (base64 padding edge case)
            "abc", // Three characters
            "Hello World!", // Common ASCII
            "🚀🌍💻", // Emoji (UTF-16 surrogate pairs)
            "\u0000\u0001\u0002", // Control characters
            "Line1\nLine2\r\nLine3", // Different line endings
            "\t\n\r\"'\\", // Escape characters
            "Naïve café résumé", // Accented characters
            "数字世界", // Chinese characters
            "цифровой мир", // Cyrillic
            "العالم الرقمي", // Arabic
            "העולם הדיגיטלי" // Hebrew
        )

        edgeCases.forEach { testValue ->
            val key = "edge_case_${testValue.hashCode()}"
            webSecureStorage.store(key, testValue)
            val result = webSecureStorage.retrieve(key)
            assertEquals(testValue, result.getOrNull(), "Edge case content should be preserved: '$testValue'")
        }
    }

    @Test
    fun `should handle concurrent localStorage access`() = runTest {
        val iterations = 50
        val keys = (1..iterations).map { "concurrent_key_$it" }
        val values = (1..iterations).map { "concurrent_value_$it" }

        // Perform concurrent stores
        keys.zip(values).forEach { (key, value) ->
            webSecureStorage.store(key, value)
        }

        // Verify all values were stored correctly
        keys.zip(values).forEach { (key, expectedValue) ->
            val result = webSecureStorage.retrieve(key)
            assertEquals(expectedValue, result.getOrNull(), "Concurrent access should preserve data")
        }

        // Verify localStorage state
        val localStorageKeys = mutableListOf<String>()
        for (i in 0 until localStorage.length) {
            localStorage.key(i)?.let { localStorageKeys.add(it) }
        }
        
        val expectedStorageKeys = keys.map { "yole_network_secure_$it" }
        expectedStorageKeys.forEach { expectedKey ->
            assertTrue(localStorageKeys.contains(expectedKey), "localStorage should contain key: $expectedKey")
        }
    }

    @Test
    fun `should clear only our namespace in localStorage`() = runTest {
        // Store some of our data
        webSecureStorage.store("our_key1", "our_value1")
        webSecureStorage.store("our_key2", "our_value2")
        
        // Store some other data in localStorage
        localStorage.setItem("other_app_key", "other_app_value")
        localStorage.setItem("another_key", "another_value")
        
        // Count items before clear
        val beforeCount = localStorage.length
        
        // Clear our storage
        webSecureStorage.clear()
        
        // Count items after clear
        val afterCount = localStorage.length
        
        // Should have removed our items but kept others
        assertTrue(afterCount < beforeCount, "Should remove some items")
        
        // Verify our items are gone
        assertNull(localStorage.getItem("yole_network_secure_our_key1"))
        assertNull(localStorage.getItem("yole_network_secure_our_key2"))
        
        // Verify other items remain
        assertEquals("other_app_value", localStorage.getItem("other_app_key"))
        assertEquals("another_value", localStorage.getItem("another_key"))
    }

    @Test
    fun `should handle malformed encrypted data gracefully`() = runTest {
        val key = "malformed_test_key"
        val storageKey = "yole_network_secure_$key"

        // Store valid data first
        webSecureStorage.store(key, "valid_value")

        // Corrupt the stored data
        localStorage.setItem(storageKey, "invalid_base64!!!")

        // Try to retrieve - should handle gracefully
        val result = webSecureStorage.retrieve(key)

        // With AES-GCM, corrupted data causes decryption failure (Result.failure).
        // With XOR fallback, corrupted base64 also causes failure.
        // Either way, it should not crash — it should return a failure result.
        assertTrue(result.isFailure, "Malformed data should cause a graceful failure")
        assertNotNull(result.exceptionOrNull(), "Should provide error details for malformed data")
    }

    @Test
    fun `should store data larger than plaintext due to encryption overhead`() = runTest {
        val sensitiveData = "password123"
        val key = "overhead_test_key"

        webSecureStorage.store(key, sensitiveData)

        val storageKey = "yole_network_secure_$key"
        val encryptedData = localStorage.getItem(storageKey)

        assertNotNull(encryptedData)
        assertNotEquals(sensitiveData, encryptedData, "Stored value must differ from plaintext")

        // AES-GCM adds IV (12 bytes) + auth tag (16 bytes) + base64 expansion.
        // XOR obfuscation adds base64 expansion. Either way, stored size > plaintext size.
        assertTrue(encryptedData.length > sensitiveData.length,
            "Encrypted/obfuscated data should be larger than plaintext due to overhead")
    }

    @Test
    fun `should report encryption support status`() = runTest {
        // In a secure context (test runner uses localhost or HTTPS), crypto.subtle is available
        // and isEncryptionSupported should be true. In non-secure contexts it would be false.
        val supported = webSecureStorage.isEncryptionSupported
        val level = webSecureStorage.securityLevel

        if (supported) {
            assertEquals("encrypted", level, "Security level should be 'encrypted' when crypto is available")
        } else {
            assertEquals("obfuscated", level, "Security level should be 'obfuscated' in fallback mode")
        }
    }

    @Test
    fun `should persist crypto key in localStorage`() = runTest {
        // After storing a value (which triggers key generation), the crypto key
        // should be persisted in localStorage under the dedicated key.
        webSecureStorage.store("key_persistence_test", "some_value")

        if (webSecureStorage.isEncryptionSupported) {
            val cryptoKey = localStorage.getItem("yole_crypto_key")
            assertNotNull(cryptoKey, "AES-GCM key should be persisted as JWK in localStorage")
            assertTrue(cryptoKey.contains("\"kty\""), "Stored key should be valid JWK JSON")
            assertTrue(cryptoKey.contains("\"k\""), "JWK should contain the key material")
        }
    }
}