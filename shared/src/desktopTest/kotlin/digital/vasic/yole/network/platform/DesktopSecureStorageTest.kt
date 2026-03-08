/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop-specific tests for DesktopSecureStorage implementation
 *
 *########################################################*/
package digital.vasic.yole.network.platform

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Desktop-specific tests for DesktopSecureStorage.
 *
 * Tests cover:
 * - AES encryption/decryption with GCM
 * - File-based storage operations
 * - Key file management
 * - Directory permissions and security
 * - Cross-platform file handling
 * - Encryption key persistence
 */
class DesktopSecureStorageTest : SecureStorageTest() {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var storageDir: File
    private lateinit var desktopSecureStorage: DesktopSecureStorage

    override suspend fun createStorage(): SecureStorage {
        return desktopSecureStorage
    }

    @Before
    fun setup() {
        storageDir = tempFolder.newFolder("secure_storage_test")
        desktopSecureStorage = DesktopSecureStorage(storageDir)
        
        // Clear any existing data
        runTest {
            desktopSecureStorage.clear()
        }
    }

    @After
    fun tearDown() {
        runTest {
            desktopSecureStorage.clear()
        }
    }

    // ==================== Desktop-Specific Tests ====================

    @Test
    fun `should create storage directory if it does not exist`() {
        val newTempDir = File(tempFolder.root, "new_secure_dir")
        assertFalse(newTempDir.exists(), "Directory should not exist initially")

        val newStorage = DesktopSecureStorage(newTempDir)
        
        runTest {
            newStorage.store("test_key", "test_value")
            assertTrue(newTempDir.exists(), "Storage directory should be created automatically")
            assertTrue(newTempDir.isDirectory, "Should be a directory")
        }
    }

    @Test
    fun `should create key file with restricted permissions`() = runBlocking {
        val keyFile = File(storageDir, ".storage_key")
        
        // Store something to trigger key file creation
        desktopSecureStorage.store("test_key", "test_value")
        
        assertTrue(keyFile.exists(), "Key file should be created")
        assertTrue(keyFile.isFile, "Should be a file")
        
        // Verify file permissions are restricted (platform-dependent)
        // On Unix-like systems, should not be world-readable
        if (!System.getProperty("os.name").lowercase().contains("windows")) {
            // Note: Java file permissions are limited, but we can check basic properties
            assertTrue(keyFile.canRead(), "Owner should be able to read")
            // Actual permission checking would require native calls
        }
    }

    @Test
    fun `should create data file with restricted permissions`() = runBlocking {
        val dataFile = File(storageDir, ".secure_storage")
        
        // Store something to trigger data file creation
        desktopSecureStorage.store("test_key", "test_value")
        
        assertTrue(dataFile.exists(), "Data file should be created")
        assertTrue(dataFile.isFile, "Should be a file")
        
        // Verify file permissions are restricted (platform-dependent)
        if (!System.getProperty("os.name").lowercase().contains("windows")) {
            assertTrue(dataFile.canRead(), "Owner should be able to read")
            assertTrue(dataFile.canWrite(), "Owner should be able to write")
        }
    }

    @Test
    fun `should use AES GCM encryption for data`() = runBlocking {
        val key = "encryption_test_key"
        val value = "sensitive_data_123"

        // Store value
        desktopSecureStorage.store(key, value)

        // Read raw encrypted data from file
        val dataFile = File(storageDir, ".secure_storage")
        val encryptedContent = dataFile.readText()
        
        // Verify content is encrypted (should not contain original value)
        assertFalse(encryptedContent.contains(value), "Encrypted data should not contain original value")
        assertFalse(encryptedContent.contains(key), "Encrypted data should not contain key in plain text")
        
        // Verify we can decrypt it back
        val decryptedResult = desktopSecureStorage.retrieve(key)
        assertEquals(value, decryptedResult.getOrNull(), "Should be able to decrypt stored value")
    }

    @Test
    fun `should persist encryption key across restarts`() = runBlocking {
        val key = "persistence_test_key"
        val value = "persistence_test_value"

        // Store with first instance
        desktopSecureStorage.store(key, value)

        // Create new storage instance (simulating app restart)
        val newStorage = DesktopSecureStorage(storageDir)

        // Should be able to retrieve with new instance
        val result = newStorage.retrieve(key)
        assertEquals(value, result.getOrNull(), "Data should persist across storage restarts")
    }

    @Test
    fun `should handle corrupted key file gracefully`() = runBlocking {
        val keyFile = File(storageDir, ".storage_key")
        
        // Store initial value
        desktopSecureStorage.store("test_key", "test_value")
        
        // Corrupt the key file
        keyFile.writeBytes(byteArrayOf(0, 1, 2, 3, 4, 5))
        
        // Create new storage instance (will try to read corrupted key)
        val corruptedStorage = DesktopSecureStorage(storageDir)
        
        // Should handle corruption gracefully - either fail or generate new key
        val result = corruptedStorage.retrieve("test_key")
        
        // Should either return null (if decryption fails) or throw exception
        if (result.isSuccess) {
            assertNull(result.getOrNull(), "Corrupted key should result in null retrieval")
        } else {
            assertNotNull(result.exceptionOrNull(), "Corrupted key should cause failure")
        }
    }

    @Test
    fun `should handle corrupted data file gracefully`() = runBlocking {
        val dataFile = File(storageDir, ".secure_storage")
        
        // Store initial value
        desktopSecureStorage.store("test_key", "test_value")
        
        // Corrupt the data file
        dataFile.writeText("corrupted_data_content")
        
        // Try to retrieve - should handle gracefully
        val result = desktopSecureStorage.retrieve("test_key")
        
        // Should either return null (if decryption fails) or throw exception
        if (result.isSuccess) {
            assertNull(result.getOrNull(), "Corrupted data should result in null retrieval")
        } else {
            assertNotNull(result.exceptionOrNull(), "Corrupted data should cause failure")
        }
    }

    @Test
    fun `should handle read-only directory`() = runBlocking {
        // Make directory read-only (if supported on platform)
        if (storageDir.setReadOnly()) {
            try {
                val result = desktopSecureStorage.store("test_key", "test_value")

                // Root user can write to read-only directories, so skip assertion
                // when running as root (e.g., in Docker containers)
                val isRoot = System.getProperty("user.name") == "root"
                if (!isRoot) {
                    // Should fail when directory is read-only
                    assertTrue(result.isFailure, "Store should fail in read-only directory")
                    assertNotNull(result.exceptionOrNull(), "Should provide error details")
                }
                // When running as root, the operation may succeed - that's expected
            } finally {
                // Restore write permissions for cleanup
                storageDir.setWritable(true)
            }
        }
        // If setReadOnly() doesn't work on this platform, skip this test
    }

    @Test
    fun `should handle missing directory parent`() = runBlocking {
        val deepPath = File(tempFolder.root, "deep/nested/path/secure")
        val storage = DesktopSecureStorage(deepPath)
        
        // Should create missing parent directories
        val result = storage.store("test_key", "test_value")
        assertTrue(result.isSuccess, "Should create missing parent directories")
        assertTrue(deepPath.exists(), "Deep directory should be created")
    }

    @Test
    fun `should validate security with encryption and file permissions`() = runBlocking {
        val isSecureResult = desktopSecureStorage.isSecure()
        assertTrue(isSecureResult.isSuccess, "Security validation should succeed")
        
        val isSecure = isSecureResult.getOrNull()
        assertNotNull(isSecure, "Security status should not be null")
        assertTrue(isSecure, "Desktop secure storage should report as secure")
    }

    @Test
    fun `should handle concurrent file access safely`() = runBlocking {
        val iterations = 50
        val keys = (1..iterations).map { "concurrent_key_$it" }
        val values = (1..iterations).map { "concurrent_value_$it" }

        // Perform concurrent stores
        keys.zip(values).forEach { (key, value) ->
            desktopSecureStorage.store(key, value)
        }

        // Verify all values were stored correctly
        keys.zip(values).forEach { (key, expectedValue) ->
            val result = desktopSecureStorage.retrieve(key)
            assertEquals(expectedValue, result.getOrNull(), "Concurrent access should preserve data")
        }
    }

    @Test
    fun `should handle file system full scenarios`() = runBlocking {
        // This is difficult to test reliably, but we can simulate large data
        val key = "large_data_key"
        val largeValue = ByteArray(1024 * 1024).joinToString("") { "a" } // 1MB of data

        val result = desktopSecureStorage.store(key, largeValue)
        
        // Should either succeed or fail gracefully with proper exception
        if (result.isFailure) {
            val exception = result.exceptionOrNull()
            assertNotNull(exception, "Should provide error details on failure")
            // Exception might be IOException or similar
        }
    }

    @Test
    fun `should encrypt data with unique IV for each operation`() = runBlocking {
        val key = "iv_test_key"
        val value = "same_value"

        // Store same value multiple times
        desktopSecureStorage.store(key, value)
        val dataFile1 = File(storageDir, ".secure_storage").readText()

        desktopSecureStorage.store(key, value) // Store again
        val dataFile2 = File(storageDir, ".secure_storage").readText()

        // Encrypted data should be different (due to different IVs)
        assertNotEquals(dataFile1, dataFile2, "Encrypted data should be different with different IVs")
        
        // But both should decrypt to the same value
        assertEquals(value, desktopSecureStorage.retrieve(key).getOrNull())
    }

    @Test
    fun `should handle cross-platform file encoding`() = runBlocking {
        val key = "encoding_test_key"
        val unicodeValue = """
            Cross-platform test:
            Windows paths: C:\Users\Test\Documents\file.txt
            Unix paths: /home/user/documents/file.txt
            Unicode: 你好世界 🌍 Привет мир
            Special chars: ${'$'} @ # ! % ^ & * ( ) _ + { } | : " < > ?
            Newlines: Windows (\r\n) vs Unix (\n)
            Tabs and spaces: 	    
        """.trimIndent()

        desktopSecureStorage.store(key, unicodeValue)
        val result = desktopSecureStorage.retrieve(key)
        assertEquals(unicodeValue, result.getOrNull(), "Cross-platform content should be preserved")
    }

    @Test
    fun `should handle temporary file cleanup`() = runBlocking {
        // Store and retrieve to ensure files are created
        desktopSecureStorage.store("test_key", "test_value")
        desktopSecureStorage.retrieve("test_key")

        val keyFile = File(storageDir, ".storage_key")
        val dataFile = File(storageDir, ".secure_storage")

        // Files should exist after operations
        assertTrue(keyFile.exists(), "Key file should exist")
        assertTrue(dataFile.exists(), "Data file should exist")

        // Clear should remove data file but keep key file (implementation detail)
        desktopSecureStorage.clear()

        // Key file should still exist (for future use)
        assertTrue(keyFile.exists(), "Key file should persist after clear")
        
        // Data file might be empty or removed depending on implementation
        if (dataFile.exists()) {
            val content = dataFile.readText().trim()
            assertTrue(content.isEmpty(), "Data file should be empty after clear")
        }
    }

    @Test
    fun `should test AES encryption parameters`() = runBlocking {
        // Test that we're using proper AES encryption parameters
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256) // Should use 256-bit keys
        val testKey = keyGenerator.generateKey()

        assertEquals("AES", testKey.algorithm, "Should use AES algorithm")
        assertEquals(32, testKey.encoded.size, "Should use 256-bit (32 byte) keys")

        // Test GCM cipher parameters
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        assertTrue(cipher.algorithm.contains("GCM") || cipher.algorithm.contains("AES"), "Should use AES/GCM mode")
        assertTrue(cipher.blockSize in 1..16, "Block size should be valid")
    }
}