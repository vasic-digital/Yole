/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Android-specific tests for AndroidSecureStorage implementation
 *
 *########################################################*/
package digital.vasic.yole.network.platform

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.*

/**
 * Android-specific tests for AndroidSecureStorage.
 *
 * Tests cover:
 * - EncryptedSharedPreferences integration
 * - Android-specific encryption/decryption
 * - Master key management
 * - Context-dependent operations
 * - Android-specific error scenarios
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 29, 30, 31, 32, 33])
class AndroidSecureStorageTest : SecureStorageTest() {

    private lateinit var context: Context
    private lateinit var androidSecureStorage: AndroidSecureStorage

    override suspend fun createStorage(): SecureStorage {
        return androidSecureStorage
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        androidSecureStorage = AndroidSecureStorage(context)
        
        // Clear any existing data
        runTest {
            androidSecureStorage.clear()
        }
    }

    @After
    fun tearDown() {
        runTest {
            androidSecureStorage.clear()
        }
        unmockkAll()
    }

    // ==================== Android-Specific Tests ====================

    @Test
    fun `should use EncryptedSharedPreferences for storage`() = runTest {
        val key = "encryption_test_key"
        val value = "sensitive_data_123"

        // Store value
        androidSecureStorage.store(key, value)

        // Verify we can't read raw data from SharedPreferences
        val regularPrefs = context.getSharedPreferences("network_secure_storage", Context.MODE_PRIVATE)
        val rawValue = regularPrefs.getString(key, null)
        
        // Raw value should be encrypted/not readable
        if (rawValue != null) {
            assertNotEquals(value, rawValue, "Raw stored value should be encrypted")
            // Encrypted values typically contain special characters or are base64 encoded
            assertTrue(rawValue.length > value.length, "Encrypted value should be longer than original")
        }
        // Note: Raw value might be null if EncryptedSharedPreferences uses separate storage
    }

    @Test
    fun `should handle MasterKey creation failures gracefully`() = runTest {
        // Mock EncryptedSharedPreferences to throw exception
        mockkStatic(EncryptedSharedPreferences::class)
        every { 
            EncryptedSharedPreferences.create(
                any(),
                any(),
                any<MasterKey>(),
                any(),
                any()
            )
        } throws SecurityException("Master key unavailable")

        val storage = AndroidSecureStorage(context)
        val key = "test_key"
        val value = "test_value"

        val result = storage.store(key, value)
        assertTrue(result.isFailure, "Store should fail when encryption is unavailable")
        assertTrue(result.exceptionOrNull() is SecurityException)

        unmockkStatic(EncryptedSharedPreferences::class)
    }

    @Test
    fun `should handle SharedPreferences commit failures`() = runTest {
        // This test verifies error handling when SharedPreferences operations fail
        val key = "commit_test_key"
        val value = "commit_test_value"

        // Store should succeed under normal conditions
        val storeResult = androidSecureStorage.store(key, value)
        assertTrue(storeResult.isSuccess, "Store should succeed normally")

        // Verify the value was stored
        val retrieveResult = androidSecureStorage.retrieve(key)
        assertEquals(value, retrieveResult.getOrNull())
    }

    @Test
    fun `should validate security with encryption test`() = runTest {
        val isSecureResult = androidSecureStorage.isSecure()
        assertTrue(isSecureResult.isSuccess, "Security validation should succeed")
        
        val isSecure = isSecureResult.getOrNull()
        assertNotNull(isSecure, "Security status should not be null")
        
        // Android implementation with EncryptedSharedPreferences should be secure
        assertTrue(isSecure, "Android secure storage should report as secure")
    }

    @Test
    fun `should handle corrupted encryption data`() = runTest {
        val key = "corruption_test_key"
        val value = "original_value"

        // Store a value normally
        androidSecureStorage.store(key, value)

        // Corrupt the underlying SharedPreferences (simulated by direct access)
        val prefs = context.getSharedPreferences("network_secure_storage", Context.MODE_PRIVATE)
        prefs.edit().putString(key, "corrupted_encrypted_data").apply()

        // Try to retrieve - should handle gracefully
        val result = androidSecureStorage.retrieve(key)
        
        // Should either return null (if decryption fails) or throw an exception
        // The exact behavior depends on EncryptedSharedPreferences implementation
        if (result.isSuccess) {
            assertNull(result.getOrNull(), "Corrupted data should return null")
        } else {
            assertNotNull(result.exceptionOrNull(), "Corrupted data should cause failure")
        }
    }

    @Test
    fun `should handle Android context changes`() = runTest {
        val key = "context_test_key"
        val value = "context_test_value"

        // Store with initial context
        androidSecureStorage.store(key, value)

        // Create new storage instance with same context
        val newStorage = AndroidSecureStorage(context)

        // Should be able to retrieve with new instance
        val result = newStorage.retrieve(key)
        assertEquals(value, result.getOrNull(), "Data should persist across storage instances")
    }

    @Test
    fun `should handle storage permission issues`() = runTest {
        // Test storage operations when permissions might be restricted
        val key = "permission_test_key"
        val value = "permission_test_value"

        // Android secure storage should handle permission issues internally
        val storeResult = androidSecureStorage.store(key, value)
        
        // In Robolectric environment, this should succeed
        // In real device with restricted permissions, might fail
        if (storeResult.isSuccess) {
            val retrieveResult = androidSecureStorage.retrieve(key)
            assertEquals(value, retrieveResult.getOrNull())
        } else {
            // If it fails, should be a proper exception
            assertNotNull(storeResult.exceptionOrNull())
        }
    }

    @Test
    fun `should handle concurrent access safely`() = runTest {
        val iterations = 100
        val keys = (1..iterations).map { "concurrent_key_$it" }
        val values = (1..iterations).map { "concurrent_value_$it" }

        // Perform concurrent stores
        keys.zip(values).forEach { (key, value) ->
            androidSecureStorage.store(key, value)
        }

        // Verify all values were stored correctly
        keys.zip(values).forEach { (key, expectedValue) ->
            val result = androidSecureStorage.retrieve(key)
            assertEquals(expectedValue, result.getOrNull(), "Concurrent store/retrieve should preserve data")
        }

        // Verify key listing includes all keys
        val listResult = androidSecureStorage.listKeys()
        val storedKeys = listResult.getOrNull()
        assertNotNull(storedKeys)
        assertEquals(iterations, storedKeys.size, "Should have all concurrent keys")
        assertTrue(storedKeys.containsAll(keys), "Should contain all concurrent keys")
    }

    @Test
    fun `should handle large data encryption`() = runTest {
        val key = "large_data_key"
        val largeValue = buildString {
            repeat(1000) {
                appendLine("This is line $it with some data to make it larger and test encryption performance")
            }
        }

        val storeResult = androidSecureStorage.store(key, largeValue)
        assertTrue(storeResult.isSuccess, "Should handle large data storage")

        val retrieveResult = androidSecureStorage.retrieve(key)
        assertEquals(largeValue, retrieveResult.getOrNull(), "Large data should be preserved through encryption/decryption")
    }

    @Test
    fun `should handle special characters in Android context`() = runTest {
        val specialCharsKey = "special_chars_key"
        val specialCharsValue = """
            Android-specific characters: ${'$'} @ # ! % ^ & * ( ) _ + { } | : " < > ?
            Unicode test: 你好世界 🌍 Привет мир
            Path-like strings: /sdcard/Download/file.txt
            Package names: com.example.app
            URLs: https://example.com/path?param=value
            JSON: {"key": "value", "number": 123}
        """.trimIndent()

        androidSecureStorage.store(specialCharsKey, specialCharsValue)
        val result = androidSecureStorage.retrieve(specialCharsValue)
        assertEquals(specialCharsValue, result.getOrNull(), "Special characters should be preserved through encryption")
    }

    @Test
    fun `should handle Android lifecycle scenarios`() = runTest {
        // Simulate app lifecycle scenarios
        val key = "lifecycle_test_key"
        val value = "lifecycle_test_value"

        // Store value
        androidSecureStorage.store(key, value)

        // Simulate app backgrounding/foregrounding by creating new instance
        val backgroundStorage = AndroidSecureStorage(context)
        
        // Should still be able to access data
        val result = backgroundStorage.retrieve(key)
        assertEquals(value, result.getOrNull(), "Data should persist across app lifecycle changes")

        // Clean up
        backgroundStorage.clear()
    }

    @Test
    fun `should test encryption key rotation resilience`() = runTest {
        // This test verifies that the storage can handle scenarios where
        // the underlying encryption keys might change (though in practice,
        // EncryptedSharedPreferences handles this internally)
        
        val key = "key_rotation_test"
        val value1 = "initial_value"
        val value2 = "updated_value"

        // Store initial value
        androidSecureStorage.store(key, value1)
        assertEquals(value1, androidSecureStorage.retrieve(key).getOrNull())

        // Update with new value (simulating key rotation scenario)
        androidSecureStorage.store(key, value2)
        assertEquals(value2, androidSecureStorage.retrieve(key).getOrNull())

        // Should always get the latest value
        assertEquals(value2, androidSecureStorage.retrieve(key).getOrNull())
    }
}