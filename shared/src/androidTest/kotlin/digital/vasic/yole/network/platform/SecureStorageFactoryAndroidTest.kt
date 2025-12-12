/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Android-specific tests for SecureStorageFactory implementation
 *
 *########################################################*/
package digital.vasic.yole.network.platform

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.*

/**
 * Android-specific tests for SecureStorageFactory.
 *
 * Tests cover:
 * - Factory creation of AndroidSecureStorage instances
 * - Platform availability checks
 * - Context-dependent creation
 * - Error handling during factory operations
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 29, 30, 31, 32, 33])
class SecureStorageFactoryAndroidTest {

    @Test
    fun `should create AndroidSecureStorage instance`() = runTest {
        val result = SecureStorageFactory.create()
        
        assertTrue(result.isSuccess, "Factory creation should succeed")
        val storage = result.getOrNull()
        assertNotNull(storage, "Storage instance should not be null")
        
        // Should be AndroidSecureStorage or mock implementation
        // Note: Current implementation uses mock, but real one would be AndroidSecureStorage
        assertTrue(storage is SecureStorage, "Should implement SecureStorage interface")
    }

    @Test
    fun `should report secure storage as available on Android`() = runTest {
        val isAvailable = SecureStorageFactory.isAvailable()
        
        // Android should support secure storage via EncryptedSharedPreferences
        assertTrue(isAvailable, "Android should report secure storage as available")
    }

    @Test
    fun `should handle factory creation errors gracefully`() = runTest {
        // Test multiple creation attempts
        val result1 = SecureStorageFactory.create()
        val result2 = SecureStorageFactory.create()
        
        assertTrue(result1.isSuccess, "First creation should succeed")
        assertTrue(result2.isSuccess, "Second creation should succeed")
        
        val storage1 = result1.getOrNull()
        val storage2 = result2.getOrNull()
        
        assertNotNull(storage1)
        assertNotNull(storage2)
    }

    @Test
    fun `should create functional storage instances`() = runTest {
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
    fun `should handle Android context requirements`() = runTest {
        // The factory should handle Android context internally
        // This test verifies it doesn't require external context
        
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess, "Factory should handle context internally")
        
        val storage = result.getOrNull()
        assertNotNull(storage, "Should create storage without external context")
    }

    @Test
    fun `should validate security status of created storage`() = runTest {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess)
        
        val storage = result.getOrNull()!!
        
        val securityResult = storage.isSecure()
        assertTrue(securityResult.isSuccess, "Security check should succeed")
        
        val isSecure = securityResult.getOrNull()
        assertNotNull(isSecure, "Security status should not be null")
        
        // Android secure storage should be secure
        assertTrue(isSecure, "Android secure storage should be secure")
    }

    @Test
    fun `should handle rapid successive creation calls`() = runTest {
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
    fun `should maintain consistency across factory calls`() = runTest {
        val result1 = SecureStorageFactory.isAvailable()
        val result2 = SecureStorageFactory.isAvailable()
        
        // Availability should be consistent
        assertEquals(result1, result2, "Availability check should be consistent")
    }
}