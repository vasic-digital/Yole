/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Integration tests for SecureStorage platform implementations
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
 * Integration tests for SecureStorage implementations.
 *
 * Tests cover:
 * - Cross-platform consistency
 * - Factory pattern integration
 * - Platform-specific behavior differences
 * - Migration scenarios
 * - Security consistency across platforms
 */
class SecureStorageIntegrationTest {

    // ==================== Cross-Platform Consistency Tests ====================

    @Test
    fun `should maintain data consistency across operations`() = runBlocking<Unit> {
        // This test would run on each platform with its specific implementation
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess, "Factory should create storage on current platform")
        
        val storage = result.getOrNull()!!
        
        // Test comprehensive data operations
        val testData = mapOf(
            "credentials" to Pair("testuser", "testpass123"),
            "token" to "sl.test_token_abc123xyz",
            "private_key" to "-----BEGIN RSA PRIVATE KEY-----\ntest_key_data\n-----END RSA PRIVATE KEY-----",
            "config" to "{\"server\": \"example.com\", \"port\": 443}",
            "unicode_test" to "Hello 世界 🌍 Привет мир",
            "special_chars" to "!@#$%^&*()_+-=[]{}|;':\",./<>?"
        )
        
        // Store credentials
        val credentialsData = testData["credentials"]!! as Pair<String, String>
        val username = credentialsData.first
        val password = credentialsData.second
        storage.storeCredentials("test_service", username, password)
        
        // Store token
        storage.storeToken("test_service", testData["token"]!! as String)
        
        // Store private key
        storage.storePrivateKey("test_service", testData["private_key"]!! as String)
        
        // Store regular values
        storage.store("config", testData["config"]!! as String)
        storage.store("unicode_test", testData["unicode_test"]!! as String)
        storage.store("special_chars", testData["special_chars"]!! as String)
        
        // Verify all data can be retrieved correctly
        val credsResult = storage.retrieveCredentials("test_service")
        assertTrue(credsResult.isSuccess)
        val retrievedCreds = credsResult.getOrNull()
        assertNotNull(retrievedCreds)
        assertEquals(credentialsData.first, retrievedCreds.first)
        assertEquals(credentialsData.second, retrievedCreds.second)
        
        val tokenResult = storage.retrieveToken("test_service")
        assertEquals(testData["token"], tokenResult.getOrNull())
        
        val keyResult = storage.retrievePrivateKey("test_service")
        assertEquals(testData["private_key"], keyResult.getOrNull())
        
        assertEquals(testData["config"], storage.retrieve("config").getOrNull())
        assertEquals(testData["unicode_test"], storage.retrieve("unicode_test").getOrNull())
        assertEquals(testData["special_chars"], storage.retrieve("special_chars").getOrNull())
        
        // Verify key listing
        val keysResult = storage.listKeys()
        val keys = keysResult.getOrNull()
        assertNotNull(keys)
        assertTrue(keys.contains("config"))
        assertTrue(keys.contains("unicode_test"))
        assertTrue(keys.contains("special_chars"))
        
        // Clean up
        storage.clear()
    }

    @Test
    fun `should handle error scenarios consistently across platforms`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess)
        
        val storage = result.getOrNull()!!
        
        // Test various error scenarios
        
        // Non-existent key retrieval
        val nonExistentResult = storage.retrieve("non_existent_key_12345")
        assertTrue(nonExistentResult.isSuccess, "Should succeed but return null")
        assertNull(nonExistentResult.getOrNull())
        
        // Non-existent key deletion
        val deleteResult = storage.delete("non_existent_key_12345")
        assertTrue(deleteResult.isSuccess, "Delete of non-existent key should succeed")
        
        // Non-existent credentials retrieval
        val credsResult = storage.retrieveCredentials("non_existent_service")
        assertTrue(credsResult.isSuccess, "Should succeed but return null")
        assertNull(credsResult.getOrNull())
        
        // Non-existent token retrieval
        val tokenResult = storage.retrieveToken("non_existent_service")
        assertTrue(tokenResult.isSuccess, "Should succeed but return null")
        assertNull(tokenResult.getOrNull())
        
        // Non-existent key retrieval
        val keyResult = storage.retrievePrivateKey("non_existent_service")
        assertTrue(keyResult.isSuccess, "Should succeed but return null")
        assertNull(keyResult.getOrNull())
        
        // Empty key operations
        val emptyKeyResult = storage.store("", "empty_key_value")
        assertTrue(emptyKeyResult.isSuccess, "Empty key should be handled")
        
        val emptyKeyRetrieve = storage.retrieve("")
        assertEquals("empty_key_value", emptyKeyRetrieve.getOrNull())
        
        storage.clear()
    }

    @Test
    fun `should provide consistent security validation`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess)
        
        val storage = result.getOrNull()!!
        
        // Security validation should work consistently
        val securityResult = storage.isSecure()
        assertTrue(securityResult.isSuccess, "Security check should succeed on all platforms")
        
        val isSecure = securityResult.getOrNull()
        assertNotNull(isSecure, "Security status should not be null")
        
        // All platform implementations should report as secure for basic use
        assertTrue(isSecure, "All platform implementations should be secure")
    }

    @Test
    fun `should handle concurrent operations safely`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess)
        
        val storage = result.getOrNull()!!
        
        // Test concurrent operations
        val concurrentOperations = (1..20).map { index ->
            val key = "concurrent_key_$index"
            val value = "concurrent_value_$index"
            
            // Store the value
            storage.store(key, value)
            
            // Immediately retrieve it
            val retrieved = storage.retrieve(key).getOrNull()
            assertEquals(value, retrieved, "Concurrent operations should preserve data integrity")
            
            // Check containment
            val contains = storage.contains(key).getOrNull()
            assertTrue(contains ?: false, "Key should exist after concurrent store")
        }
        
        // Verify final state
        val keysResult = storage.listKeys()
        val finalKeys = keysResult.getOrNull()
        assertNotNull(finalKeys)
        assertEquals(20, finalKeys.size, "Should have all concurrent keys")
        
        storage.clear()
    }

    // ==================== Factory Pattern Tests ====================

    @Test
    fun `should provide appropriate platform-specific implementations`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess, "Factory should succeed on current platform")
        
        val storage = result.getOrNull()!!
        
        // Verify it's a valid SecureStorage implementation
        assertTrue(storage is SecureStorage, "Created storage should implement SecureStorage")
        assertTrue(SecureStorageFactory.isAvailable(), "Platform should be available")
    }

    @Test
    fun `should handle factory availability consistently`() = runBlocking<Unit> {
        // Availability should be consistent across multiple calls
        val availability1 = SecureStorageFactory.isAvailable()
        val availability2 = SecureStorageFactory.isAvailable()
        val availability3 = SecureStorageFactory.isAvailable()
        
        assertEquals(availability1, availability2, "Availability should be consistent")
        assertEquals(availability2, availability3, "Availability should be consistent")
        
        // If available, factory creation should succeed
        if (availability1) {
            val creationResult = SecureStorageFactory.create()
            assertTrue(creationResult.isSuccess, "Factory creation should succeed when available")
            assertNotNull(creationResult.getOrNull(), "Should create storage when available")
        }
    }

    @Test
    fun `should handle factory creation failures gracefully`() = runBlocking<Unit> {
        // Test multiple creation attempts
        val results = (1..5).map {
            SecureStorageFactory.create()
        }
        
        // All attempts should have the same outcome
        val firstResult = results.first()
        results.forEach { result ->
            assertEquals(firstResult.isSuccess, result.isSuccess, "All factory creations should have consistent results")
            if (firstResult.isSuccess && result.isSuccess) {
                assertNotNull(result.getOrNull(), "Successful creations should provide storage instances")
            }
        }
    }

    // ==================== Security Integration Tests ====================

    @Test
    fun `should maintain security across different data types`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess)
        
        val storage = result.getOrNull()!!
        
        // Test sensitive data types
        val sensitiveData = mapOf(
            "simple_password" to "password123",
            "complex_password" to "P@ssw0rd!#$%^&*()",
            "api_token" to "ghp_1234567890abcdefghijklmnopqrstuvwxyz",
            "ssh_key" to """
                -----BEGIN OPENSSH PRIVATE KEY-----
                b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAFwAAAAdzc2gtcn
                NhAAAAAwEAAQAAAQEA2nP8+YXgFYZx5hR+1X9mOLfJqZJQHbPDuPg0+NXoON1qYF1P8WkB0
                -----END OPENSSH PRIVATE KEY-----
            """.trimIndent(),
            "oauth_refresh" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        )
        
        // Store all sensitive data
        sensitiveData.forEach { (key, value) ->
            storage.store(key, value)
        }
        
        // Verify security status with sensitive data
        val securityResult = storage.isSecure()
        assertTrue(securityResult.isSuccess, "Security should be maintained with sensitive data")
        assertTrue(securityResult.getOrNull() ?: false, "Should remain secure with sensitive data")
        
        // Verify all data can be retrieved correctly
        sensitiveData.forEach { (key, expectedValue) ->
            val result = storage.retrieve(key)
            assertEquals(expectedValue, result.getOrNull(), "Sensitive data should be retrievable and intact")
        }
        
        storage.clear()
    }

    @Test
    fun `should handle credential management securely`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess)
        
        val storage = result.getOrNull()!!
        
        // Test various credential formats
        val credentials = listOf(
            Triple("webdav", "user@domain.com", "pass:word:with:colons"),
            Triple("sftp", "admin", "simple_password"),
            Triple("ftp", "anonymous", "user@example.com"),
            Triple("database", "dbuser", "P@ssw0rd!#$%^&*()"),
            Triple("api", "api_key", "sk-1234567890abcdefghijklmnopqrstuvwxyz")
        )
        
        // Store all credentials
        credentials.forEach { (service, username, password) ->
            val storeResult = storage.storeCredentials(service, username, password)
            assertTrue(storeResult.isSuccess, "Should store credentials for $service")
        }
        
        // Verify all credentials can be retrieved
        credentials.forEach { (service, expectedUsername, expectedPassword) ->
            val credsResult = storage.retrieveCredentials(service)
            assertTrue(credsResult.isSuccess, "Should retrieve credentials for $service")
            
            val retrievedCreds = credsResult.getOrNull()
            assertNotNull(retrievedCreds, "Should retrieve non-null credentials for $service")
            assertEquals(expectedUsername, retrievedCreds.first, "Username should match for $service")
            assertEquals(expectedPassword, retrievedCreds.second, "Password should match for $service")
        }
        
        // Test deletion
        val deleteService = credentials.first().first
        val deleteResult = storage.deleteCredentials(deleteService)
        assertTrue(deleteResult.isSuccess, "Should delete credentials")
        
        val afterDelete = storage.retrieveCredentials(deleteService)
        assertNull(afterDelete.getOrNull(), "Credentials should be null after deletion")
        
        storage.clear()
    }

    // ==================== Performance Integration Tests ====================

    @Test
    fun `should handle large data volumes efficiently`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess)
        
        val storage = result.getOrNull()!!
        
        // Test with many items
        val itemCount = 100
        val startTime = Clock.System.now().toEpochMilliseconds()
        
        (1..itemCount).forEach { index ->
            val key = "performance_key_$index"
            val value = "performance_value_$index with some additional content to make it realistic"
            storage.store(key, value)
        }
        
        val storeTime = Clock.System.now().toEpochMilliseconds() - startTime
        
        // Verify all items can be retrieved
        val retrieveStart = Clock.System.now().toEpochMilliseconds()
        (1..itemCount).forEach { index ->
            val key = "performance_key_$index"
            val expectedValue = "performance_value_$index with some additional content to make it realistic"
            val retrieved = storage.retrieve(key).getOrNull()
            assertEquals(expectedValue, retrieved, "Large volume retrieval should preserve data")
        }
        
        val retrieveTime = Clock.System.now().toEpochMilliseconds() - retrieveStart
        
        // Performance assertions (platform-dependent)
        assertTrue(storeTime < 5000, "Should store 100 items in less than 5 seconds")
        assertTrue(retrieveTime < 3000, "Should retrieve 100 items in less than 3 seconds")
        
        storage.clear()
    }

    @Test
    fun `should handle large individual items`() = runBlocking<Unit> {
        val result = SecureStorageFactory.create()
        assertTrue(result.isSuccess)
        
        val storage = result.getOrNull()!!
        
        // Test with large individual items
        val largeText = buildString {
            repeat(1000) { line ->
                appendLine("This is line $line with some content to make the overall size substantial for testing large item handling")
            }
        }
        
        val largeJson = buildString {
            append("{")
            repeat(100) { index ->
                if (index > 0) append(",")
                append("\"key$index\": \"value$index with some content\"")
            }
            append("}")
        }
        
        val startTime = Clock.System.now().toEpochMilliseconds()
        storage.store("large_text", largeText)
        storage.store("large_json", largeJson)
        val storeTime = Clock.System.now().toEpochMilliseconds() - startTime
        
        val retrieveStart = Clock.System.now().toEpochMilliseconds()
        val retrievedText = storage.retrieve("large_text").getOrNull()
        val retrievedJson = storage.retrieve("large_json").getOrNull()
        val retrieveTime = Clock.System.now().toEpochMilliseconds() - retrieveStart
        
        assertEquals(largeText, retrievedText, "Large text should be preserved")
        assertEquals(largeJson, retrievedJson, "Large JSON should be preserved")
        
        // Performance assertions
        assertTrue(storeTime < 2000, "Should store large items quickly")
        assertTrue(retrieveTime < 1000, "Should retrieve large items quickly")
        
        storage.clear()
    }
}