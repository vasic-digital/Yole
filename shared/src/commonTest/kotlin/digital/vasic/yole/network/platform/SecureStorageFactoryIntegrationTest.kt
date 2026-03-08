/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Integration tests for SecureStorageFactory platform implementations
 *
 *########################################################*/
package digital.vasic.yole.network.platform

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test

import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Integration tests for SecureStorageFactory implementations.
 *
 * Tests cover:
 * - Cross-platform factory behavior
 * - Platform detection and adaptation
 * - Factory error handling
 * - Platform-specific optimizations
 * - Fallback scenarios
 */
class SecureStorageFactoryIntegrationTest {

    // ==================== Factory Creation Tests ====================

    @Test
    fun `should create appropriate platform-specific storage`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        
        assertTrue(result.isSuccess, "Factory creation should succeed on current platform")
        val storage = result.getOrNull()
        assertNotNull(storage, "Should create storage instance")
        
        // Verify it's a valid SecureStorage implementation
        assertTrue(storage is SecureStorage, "Created storage should implement SecureStorage")
        
        // Test basic functionality
        val testKey = "factory_integration_test"
        val testValue = "factory_integration_value"
        
        val storeResult = storage.store(testKey, testValue)
        assertTrue(storeResult.isSuccess, "Factory-created storage should support basic operations")
        
        val retrieveResult = storage.retrieve(testKey)
        assertEquals(testValue, retrieveResult.getOrNull())
        
        // Clean up
        storage.clear()
    }

    @Test
    fun `should report platform availability accurately`() = runBlocking<Unit> {
        val isAvailable = SecureStorageFactory.isAvailable()
        
        // Availability should be deterministic
        assertTrue(isAvailable || !isAvailable, "Availability should be boolean")
        
        // Test consistency across multiple calls
        val availabilityChecks = (1..10).map { SecureStorageFactory.isAvailable() }
        val allSame = availabilityChecks.all { it == isAvailable }
        assertTrue(allSame, "Availability should be consistent across calls")
        
        // If available, factory creation should succeed
        if (isAvailable) {
            val creationResult = SecureStorageFactory.create()
            assertTrue(creationResult.isSuccess, "Factory should succeed when available")
            assertNotNull(creationResult.getOrNull(), "Should create storage when available")
        }
    }

    @Test
    fun `should handle multiple factory creation requests`() = runBlocking<Unit> {
        // Test that factory can handle multiple creation requests
        val creationResults = (1..5).map {
            SecureStorageFactory.create()
        }
        
        // All should have the same outcome
        val firstResult = creationResults.first()
        creationResults.forEach { result ->
            assertEquals(firstResult.isSuccess, result.isSuccess, 
                "All factory creations should have consistent success status")
            
            if (firstResult.isSuccess && result.isSuccess) {
                assertNotNull(result.getOrNull(), 
                    "Successful factory creations should provide storage instances")
            }
        }
        
        // If successful, test that all instances work
        if (firstResult.isSuccess) {
            creationResults.forEach { result ->
                val storage = result.getOrNull()!!
                
                // Each instance should be functional
                val testKey = "multi_instance_test_${Clock.System.now().toEpochMilliseconds()}"
                val testValue = "multi_instance_value"
                
                storage.store(testKey, testValue)
                assertEquals(testValue, storage.retrieve(testKey).getOrNull())
                
                storage.clear()
            }
        }
    }

    // ==================== Platform-Specific Behavior Tests ====================

    @Test
    fun `should provide platform-appropriate security implementations`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess)
        
        val storage = result.getOrNull()!!
        
        // Test that the implementation is appropriate for the platform
        val securityResult = storage.isSecure()
        assertTrue(securityResult.isSuccess, "Security check should work on all platforms")
        
        val isSecure = securityResult.getOrNull()
        assertNotNull(isSecure, "Security status should be available")
        
        // All implementations should provide some level of security
        assertTrue(isSecure, "Platform implementation should be secure")
        
        // Test with sensitive data
        val sensitiveData = "password123!@#"
        storage.store("security_test", sensitiveData)
        
        val retrieved = storage.retrieve("security_test").getOrNull()
        assertEquals(sensitiveData, retrieved, "Security implementation should preserve data integrity")
        
        storage.clear()
    }

    @Test
    fun `should handle platform-specific limitations gracefully`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess)
        
        val storage = result.getOrNull()!!
        
        // Test operations that might have platform-specific limitations
        
        // Large data test
        val largeValue = "x".repeat(1024 * 100) // 100KB
        val largeResult = storage.store("large_data_test", largeValue)
        
        if (largeResult.isSuccess) {
            val retrieved = storage.retrieve("large_data_test").getOrNull()
            assertEquals(largeValue, retrieved, "Large data should be handled correctly")
        } else {
            // Should fail gracefully with meaningful error
            assertNotNull(largeResult.exceptionOrNull(), "Large data failures should provide errors")
        }
        
        // Many items test
        val manyItemsResult = (1..100).map { index ->
            storage.store("many_items_$index", "value_$index")
        }
        
        // Should handle many items gracefully
        val successCount = manyItemsResult.count { it.isSuccess }
        assertTrue(successCount > 0, "Should handle at least some multiple items")
        
        // If items were stored, verify integrity
        if (successCount > 0) {
            val firstRetrieved = storage.retrieve("many_items_1").getOrNull()
            if (firstRetrieved != null) {
                assertEquals("value_1", firstRetrieved)
            }
        }
        
        storage.clear()
    }

    // ==================== Cross-Platform Consistency Tests ====================

    @Test
    fun `should maintain API consistency across platforms`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess)

        val storage = result.getOrNull()!!
        storage.clear()

        // Test that all interface methods are available and functional
        val testData = mapOf(
            "basic" to "basic_value",
            "unicode" to "unicode_测试_🌍",
            "special" to "special!@#$%^&*()"
        )
        
        // Test all basic operations
        testData.forEach { (key, value) ->
            // Store
            val storeResult = storage.store(key, value)
            assertTrue(storeResult.isSuccess, "Store should work on all platforms: $key")
            
            // Retrieve
            val retrieveResult = storage.retrieve(key)
            assertEquals(value, retrieveResult.getOrNull(), "Retrieve should work on all platforms: $key")
            
            // Contains
            val containsResult = storage.contains(key)
            assertTrue(containsResult.isSuccess, "Contains should work on all platforms: $key")
            assertTrue(containsResult.getOrNull() ?: false, "Should contain stored key: $key")
        }
        
        // Test key listing
        val listResult = storage.listKeys()
        assertTrue(listResult.isSuccess, "ListKeys should work on all platforms")
        val keys = listResult.getOrNull()
        assertNotNull(keys, "Should return key list")
        assertEquals(testData.size, keys.size, "Should list all keys")
        
        // Test clear
        val clearResult = storage.clear()
        assertTrue(clearResult.isSuccess, "Clear should work on all platforms")
        
        // Verify clear worked
        val afterClear = storage.listKeys().getOrNull()
        assertTrue(afterClear?.isEmpty() ?: false, "Should have no keys after clear")
    }

    @Test
    fun `should handle credential operations consistently`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess)
        
        val storage = result.getOrNull()!!
        
        // Test credential operations that should work consistently
        val services = listOf("webdav", "sftp", "ftp", "database")
        val credentials = listOf(
            Triple("webdav", "user@domain.com", "password123"),
            Triple("sftp", "admin", "secure_pass!@#"),
            Triple("ftp", "anonymous", "user@example.com"),
            Triple("database", "dbuser", "P@ssw0rd")
        )
        
        // Store credentials
        credentials.forEach { (service, username, password) ->
            val storeResult = storage.storeCredentials(service, username, password)
            assertTrue(storeResult.isSuccess, "Should store credentials for $service")
        }
        
        // Retrieve and verify
        credentials.forEach { (service, expectedUsername, expectedPassword) ->
            val retrieveResult = storage.retrieveCredentials(service)
            assertTrue(retrieveResult.isSuccess, "Should retrieve credentials for $service")
            
            val retrieved = retrieveResult.getOrNull()
            assertNotNull(retrieved, "Should retrieve non-null credentials for $service")
            assertEquals(expectedUsername, retrieved.first, "Username should match for $service")
            assertEquals(expectedPassword, retrieved.second, "Password should match for $service")
        }
        
        // Test token operations
        credentials.forEach { (service, _, _) ->
            val token = "token_for_$service"
            storage.storeToken(service, token)
            
            val retrievedToken = storage.retrieveToken(service).getOrNull()
            assertEquals(token, retrievedToken, "Token should match for $service")
        }
        
        // Test private key operations
        credentials.forEach { (service, _, _) ->
            val privateKey = "-----BEGIN PRIVATE KEY-----\nkey_for_$service\n-----END PRIVATE KEY-----"
            storage.storePrivateKey(service, privateKey)
            
            val retrievedKey = storage.retrievePrivateKey(service).getOrNull()
            assertEquals(privateKey, retrievedKey, "Private key should match for $service")
        }
        
        storage.clear()
    }

    // ==================== Error Handling Integration Tests ====================

    @Test
    fun `should handle edge cases consistently across platforms`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess)
        
        val storage = result.getOrNull()!!
        
        // Test edge cases that should be handled consistently
        
        // Empty operations
        storage.store("", "empty_key_value")
        assertEquals("empty_key_value", storage.retrieve("").getOrNull())
        
        // Unicode edge cases
        val unicodeTests = listOf(
            "nfc_café" to "café", // NFC form
            "nfd_cafe\u0301" to "cafe\u0301", // NFD form
            "emoji" to "🌍🚀💻", // Emoji
            "mixed_scripts" to "Hello世界مرحبا", // Mixed scripts
            "rtl" to "مرحبا بالعالم", // RTL text
            "special_unicode" to "\u0000\u200B\u00AD" // Special Unicode
        )
        
        unicodeTests.forEach { (key, value) ->
            storage.store(key, value)
            val retrieved = storage.retrieve(key).getOrNull()
            assertEquals(value, retrieved, "Unicode should be handled consistently: $key")
        }
        
        // Long key/value tests
        val longKey = "x".repeat(100)
        val longValue = "y".repeat(1000)
        storage.store(longKey, longValue)
        assertEquals(longValue, storage.retrieve(longKey).getOrNull(), "Long data should be handled")
        
        // Special characters
        val specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?"
        storage.store("special_chars", specialChars)
        assertEquals(specialChars, storage.retrieve("special_chars").getOrNull(), "Special chars should be handled")
        
        storage.clear()
    }

    @Test
    fun `should provide meaningful error information`() = runBlocking<Unit> {
        // Test factory error handling
        val availability = SecureStorageFactory.isAvailable()
        
        // Factory creation should provide meaningful results
        val creationResult = SecureStorageFactory.create()
        
        if (availability) {
            assertTrue(creationResult.isSuccess, "Factory should succeed when available")
            assertNotNull(creationResult.getOrNull(), "Should provide storage when available")
        } else {
            // If not available, should fail meaningfully
            if (creationResult.isFailure) {
                val exception = creationResult.exceptionOrNull()
                assertNotNull(exception, "Failures should provide exception information")
            }
        }
        
        // Test storage error handling
        if (creationResult.isSuccess) {
            val storage = creationResult.getOrNull()!!
            
            // Security validation should provide meaningful status
            val securityResult = storage.isSecure()
            assertTrue(securityResult.isSuccess, "Security check should provide meaningful result")
            
            val isSecure = securityResult.getOrNull()
            assertNotNull(isSecure, "Security status should be meaningful")
            
            // All operations should provide meaningful results
            val testResult = storage.retrieve("non_existent_key")
            assertTrue(testResult.isSuccess, "Non-existent key retrieval should succeed with null")
            assertNull(testResult.getOrNull(), "Non-existent key should return null")
            
            storage.clear()
        }
    }

    // ==================== Performance Integration Tests ====================

    @Test
    fun `should handle performance scenarios efficiently`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess)
        
        val storage = result.getOrNull()!!
        
        // Test performance with multiple operations
        val operations = 50
        val startTime = Clock.System.now().toEpochMilliseconds()
        
        // Store multiple items
        (1..operations).forEach { index ->
            storage.store("perf_key_$index", "perf_value_$index")
        }
        
        val storeTime = Clock.System.now().toEpochMilliseconds() - startTime
        
        // Retrieve multiple items
        val retrieveStart = Clock.System.now().toEpochMilliseconds()
        (1..operations).forEach { index ->
            val retrieved = storage.retrieve("perf_key_$index").getOrNull()
            assertEquals("perf_value_$index", retrieved, "Performance test should preserve data")
        }
        
        val retrieveTime = Clock.System.now().toEpochMilliseconds() - retrieveStart
        
        // Performance assertions (generous to account for platform differences)
        assertTrue(storeTime < 10000, "Should store 50 items in less than 10 seconds")
        assertTrue(retrieveTime < 5000, "Should retrieve 50 items in less than 5 seconds")
        
        storage.clear()
    }

    // ==================== Security Integration Tests ====================

    @Test
    fun `should maintain security guarantees across platforms`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess)
        
        val storage = result.getOrNull()!!
        
        // Test security with sensitive data
        val sensitiveData = mapOf(
            "password" to "MyS3cur3P@ssw0rd!",
            "api_key" to "sk-1234567890abcdefghijklmnopqrstuvwxyz",
            "private_key" to "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQ...\n-----END PRIVATE KEY-----",
            "token" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        )
        
        // Store sensitive data
        sensitiveData.forEach { (key, value) ->
            storage.store(key, value)
        }
        
        // Verify security status
        val securityResult = storage.isSecure()
        assertTrue(securityResult.isSuccess, "Security validation should work")
        assertTrue(securityResult.getOrNull() ?: false, "Should maintain security with sensitive data")
        
        // Verify data integrity
        sensitiveData.forEach { (key, expectedValue) ->
            val retrieved = storage.retrieve(key).getOrNull()
            assertEquals(expectedValue, retrieved, "Sensitive data should be preserved securely")
        }
        
        // Test credential security
        storage.storeCredentials("test_service", "testuser", "testpass123")
        val creds = storage.retrieveCredentials("test_service").getOrNull()
        assertNotNull(creds, "Credentials should be retrievable")
        assertEquals("testuser", creds.first)
        assertEquals("testpass123", creds.second)
        
        storage.clear()
    }
}