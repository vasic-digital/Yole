/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop-specific tests for SecureStorageFactory implementation
 *
 *########################################################*/
package digital.vasic.yole.network.platform

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Desktop-specific tests for SecureStorageFactory.
 *
 * Tests cover:
 * - Factory creation of DesktopSecureStorage instances
 * - Platform availability checks
 * - Directory creation and permissions
 * - Cross-platform home directory detection
 * - Error handling during factory operations
 */
class SecureStorageFactoryDesktopTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `should create DesktopSecureStorage instance`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        
        assertTrue(result.isSuccess, "Factory creation should succeed")
        val storage = result.getOrNull()
        assertNotNull(storage, "Storage instance should not be null")
        
        // Should be DesktopSecureStorage implementation
        assertTrue(storage is DesktopSecureStorage, "Should create DesktopSecureStorage instance")
    }

    @Test
    fun `should report secure storage as available on desktop`() = runBlocking<Unit> {
        val isAvailable = SecureStorageFactory.isAvailable()
        
        // Desktop should support secure storage via file system and crypto
        assertTrue(isAvailable, "Desktop should report secure storage as available")
    }

    @Test
    fun `should create storage in user home directory`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess)
        
        val storage = result.getOrNull() as DesktopSecureStorage
        
        // Verify storage directory is in user home
        val storageDir = storage::class.java.getDeclaredField("storageDir").apply {
            isAccessible = true
        }.get(storage) as File
        
        val userHome = System.getProperty("user.home") ?: System.getenv("HOME") ?: "."
        assertTrue(storageDir.absolutePath.startsWith(userHome), 
            "Storage directory should be in user home")
        
        // Should be in .yole/secure subdirectory
        assertTrue(storageDir.absolutePath.contains(".yole"), "Should use .yole subdirectory")
        assertTrue(storageDir.absolutePath.contains("secure"), "Should use secure subdirectory")
    }

    @Test
    fun `should create storage directory structure`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess)
        
        val storage = result.getOrNull() as DesktopSecureStorage
        
        // Get the storage directory
        val storageDir = storage::class.java.getDeclaredField("storageDir").apply {
            isAccessible = true
        }.get(storage) as File
        
        // Directory should be created
        assertTrue(storageDir.exists(), "Storage directory should be created")
        assertTrue(storageDir.isDirectory, "Should be a directory")
        assertTrue(storageDir.canWrite(), "Should be writable")
    }

    @Test
    fun `should handle unavailable home directory`() = runBlocking<Unit> {
        // Test with custom home directory that might not exist
        val originalHome = System.getProperty("user.home")
        val originalHomeEnv = System.getenv("HOME")
        
        try {
            // Set a non-existent home directory
            val fakeHome = tempFolder.root.absolutePath + "/fake_home"
            System.setProperty("user.home", fakeHome)
            
            val result = SecureStorageFactory.create()
            assertTrue(result.isSuccess, "Should handle missing home directory")
            
            val storage = result.getOrNull()
            assertNotNull(storage, "Should create storage even with fake home")
            
        } finally {
            // Restore original home directory
            if (originalHome != null) {
                System.setProperty("user.home", originalHome)
            }
        }
    }

    @Test
    fun `should create functional storage instances`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess)
        
        val storage = result.getOrNull()!!
        
        // Test basic functionality
        val testKey = "factory_test_key"
        val testValue = "factory_test_value"
        
        // Store and retrieve
        val storeResult = storage.store(testKey, testValue)
        assertTrue(storeResult.isSuccess, "Factory-created storage should support store operations")
        
        val retrieveResult = storage.retrieve(testKey)
        assertTrue(retrieveResult.isSuccess, "Factory-created storage should support retrieve operations")
        assertEquals(testValue, retrieveResult.getOrNull())
        
        // Clean up
        storage.clear()
    }

    @Test
    fun `should handle permission issues gracefully`() = runBlocking<Unit> {
        // Test with a read-only directory
        val readOnlyDir = tempFolder.newFolder("readonly_test")
        readOnlyDir.setReadOnly()
        
        var originalHome: String? = null
        try {
            // Temporarily override home to use read-only directory
            originalHome = System.getProperty("user.home")
            System.setProperty("user.home", readOnlyDir.absolutePath)
            
            val result = SecureStorageFactory.create()
            
            // Should either succeed (if it can create subdirectory) or fail gracefully
            if (result.isFailure) {
                assertNotNull(result.exceptionOrNull(), "Should provide error details")
            }
            
        } finally {
            // Restore original home and make directory writable for cleanup
            if (originalHome != null) {
                System.setProperty("user.home", originalHome)
            }
            readOnlyDir.setWritable(true)
        }
    }

    @Test
    fun `should validate security status of created storage`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess)
        
        val storage = result.getOrNull()!!
        
        val securityResult = storage.isSecure()
        assertTrue(securityResult.isSuccess, "Security check should succeed")
        
        val isSecure = securityResult.getOrNull()
        assertNotNull(isSecure, "Security status should not be null")
        
        // Desktop secure storage with encryption should be secure
        assertTrue(isSecure, "Desktop secure storage should be secure")
    }

    @Test
    fun `should handle rapid successive creation calls`() = runBlocking<Unit> {
        // Test factory under load
        val creationResults = (1..10).map {
            SecureStorageFactory.create()
        }
        
        // All creations should succeed
        creationResults.forEach { result ->
            assertTrue(result.isSuccess, "All factory creations should succeed")
            assertNotNull(result.getOrNull(), "All storage instances should be created")
        }
    }

    @Test
    fun `should maintain consistency across factory calls`() = runBlocking<Unit> {
        val result1 = SecureStorageFactory.isAvailable()
        val result2 = SecureStorageFactory.isAvailable()
        
        // Availability should be consistent
        assertEquals(result1, result2, "Availability check should be consistent")
    }

    @Test
    fun `should handle cross-platform home directory detection`() = runBlocking<Unit> {
        // Test that factory works with different environment setups
        val originalHome = System.getProperty("user.home")
        val originalHomeEnv = System.getenv("HOME")
        
        try {
            // Test with only user.home property
            System.clearProperty("user.home")
            System.setProperty("user.home", tempFolder.root.absolutePath)
            
            val result1 = SecureStorageFactory.create()
            assertTrue(result1.isSuccess, "Should work with user.home property")
            
            // Test with HOME environment variable (Unix-like)
            if (originalHomeEnv != null) {
                System.clearProperty("user.home")
                // Note: We can't easily mock environment variables in Java
                // This part would need native testing or special setup
            }
            
        } finally {
            // Restore original settings
            if (originalHome != null) {
                System.setProperty("user.home", originalHome)
            }
        }
    }

    @Test
    fun `should create storage with proper encryption setup`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess)
        
        val storage = result.getOrNull() as DesktopSecureStorage
        
        // Store something to trigger encryption setup
        storage.store("encryption_test", "test_value")
        
        // Verify files are created
        val storageDir = storage::class.java.getDeclaredField("storageDir").apply {
            isAccessible = true
        }.get(storage) as File
        
        val keyFile = File(storageDir, ".storage_key")
        val dataFile = File(storageDir, ".secure_storage")
        
        assertTrue(keyFile.exists(), "Encryption key file should be created")
        assertTrue(dataFile.exists(), "Encrypted data file should be created")
        
        // Verify data is encrypted
        val rawData = dataFile.readText()
        assertFalse(rawData.contains("test_value"), "Data should be encrypted")
    }
}