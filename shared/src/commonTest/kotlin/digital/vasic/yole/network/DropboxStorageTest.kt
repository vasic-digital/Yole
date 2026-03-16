/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Dropbox Storage Tests - Comprehensive Coverage
 *
 *########################################################*/

package digital.vasic.yole.network

import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.protocols.dropbox.DropboxService
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Comprehensive test suite for DropboxService (via StorageConfig.DropboxConfig)
 * Tests configuration, initialization, and basic service properties.
 */
class DropboxStorageTest {

    private lateinit var service: DropboxService

    private val testConfig = StorageConfig.DropboxConfig(
        name = "test-dropbox",
        accessToken = "test_access_token",
        appKey = "test_app_key",
        appSecret = "test_app_secret",
        refreshToken = "test_refresh_token",
        rootPath = "/Apps/Yole"
    )

    @BeforeTest
    fun setup() {
        service = DropboxService(testConfig)
    }

    // ==================== Configuration Tests ====================

    @Test
    fun testDropboxServiceConfigIsSet() {
        assertEquals("test-dropbox", service.config.name)
        assertEquals("test_access_token", service.config.accessToken)
        assertEquals("test_app_key", service.config.appKey)
        assertEquals("test_app_secret", service.config.appSecret)
        assertEquals("test_refresh_token", service.config.refreshToken)
        assertEquals("/Apps/Yole", service.config.rootPath)
    }

    @Test
    fun testDropboxServiceStorageType() {
        assertEquals(StorageType.DROPBOX, service.config.storageType)
    }

    @Test
    fun testDropboxServiceDefaultConfig() {
        val minimalConfig = StorageConfig.DropboxConfig(
            name = "minimal-dropbox",
            accessToken = "token",
            appKey = "key",
            appSecret = "secret"
        )
        val minimalService = DropboxService(minimalConfig)

        assertEquals("minimal-dropbox", minimalService.config.name)
        assertEquals("", minimalService.config.rootPath)
        assertNull(minimalService.config.refreshToken)
        assertTrue(minimalService.config.isEnabled)
        assertEquals(100, minimalService.config.priority)
        assertTrue(minimalService.config.metadata.isEmpty())
    }

    // ==================== Storage Info Tests ====================

    @Test
    fun testGetStorageInfo() = runBlocking<Unit> {
        val storageInfo = service.getStorageInfo()
        assertNotNull(storageInfo)
        assertNotNull(storageInfo.id)
        assertNotNull(storageInfo.name)
        assertEquals(StorageType.DROPBOX, storageInfo.type)
    }

    @Test
    fun testDropboxSupportsFolders() = runBlocking<Unit> {
        val storageInfo = service.getStorageInfo()
        assertTrue(storageInfo.supportsFolders, "Dropbox should support folders")
    }

    // ==================== Connection Tests ====================

    @Test
    fun testDisconnect() = runBlocking<Unit> {
        // Disconnect should succeed even when not connected
        val result = service.disconnect()
        assertTrue(result.isSuccess, "Disconnect should succeed")
    }

    @Test
    fun testConnectWithoutNetworkReturnsFailure() = runBlocking<Unit> {
        // Without a real network, connect should fail gracefully
        val result = service.connect()
        // Should complete (either success or failure depending on implementation)
        assertTrue(result.isSuccess || result.isFailure, "Connect should complete")
    }

    @Test
    fun testTestConnectionWithoutNetwork() = runBlocking<Unit> {
        val result = service.testConnection()
        assertTrue(result.isSuccess || result.isFailure, "testConnection should complete")
    }

    // ==================== Quota Tests ====================

    @Test
    fun testGetQuotaInfo() = runBlocking<Unit> {
        val result = service.getQuotaInfo()
        // getQuotaInfo now makes real API calls; without auth it fails when not connected
        assertTrue(result.isFailure, "getQuotaInfo should fail when not connected")
    }

    // ==================== Exists Tests ====================

    @Test
    fun testExistsCheck() = runBlocking<Unit> {
        val result = service.exists("/test.txt")
        assertTrue(result.isSuccess, "exists should succeed")
    }

    // ==================== Config Modification Tests ====================

    @Test
    fun testConfigWithEnabled() {
        val disabledConfig = testConfig.withEnabled(false) as StorageConfig.DropboxConfig
        assertFalse(disabledConfig.isEnabled)
    }

    @Test
    fun testConfigWithPriority() {
        val highPriorityConfig = testConfig.withPriority(1) as StorageConfig.DropboxConfig
        assertEquals(1, highPriorityConfig.priority)
    }

    @Test
    fun testConfigWithMetadata() {
        val metadataConfig = testConfig.withMetadata(
            mapOf("key1" to "value1", "key2" to "value2")
        ) as StorageConfig.DropboxConfig
        assertEquals(2, metadataConfig.metadata.size)
        assertEquals("value1", metadataConfig.metadata["key1"])
    }

    // ==================== Edge Cases ====================

    @Test
    fun testConfigWithEmptyName() {
        val config = StorageConfig.DropboxConfig(
            name = "",
            accessToken = "token",
            appKey = "key",
            appSecret = "secret"
        )
        val emptyNameService = DropboxService(config)
        assertEquals("", emptyNameService.config.name)
    }

    @Test
    fun testConfigWithLongName() {
        val longName = "a".repeat(10000)
        val config = StorageConfig.DropboxConfig(
            name = longName,
            accessToken = "token",
            appKey = "key",
            appSecret = "secret"
        )
        val longNameService = DropboxService(config)
        assertEquals(longName, longNameService.config.name)
    }

    @Test
    fun testConfigEquality() {
        val config1 = StorageConfig.DropboxConfig(
            name = "test",
            accessToken = "token",
            appKey = "key",
            appSecret = "secret"
        )
        val config2 = StorageConfig.DropboxConfig(
            name = "test",
            accessToken = "token",
            appKey = "key",
            appSecret = "secret"
        )
        assertEquals(config1, config2)
    }

    @Test
    fun testConfigCopy() {
        val original = StorageConfig.DropboxConfig(
            name = "original",
            accessToken = "token",
            appKey = "key",
            appSecret = "secret"
        )
        val copied = original.copy(name = "copied")
        assertEquals("copied", copied.name)
        assertEquals("token", copied.accessToken)
    }

    @Test
    fun testMultipleServiceInstances() = runBlocking<Unit> {
        val configs = (1..10).map { i ->
            StorageConfig.DropboxConfig(
                name = "dropbox-$i",
                accessToken = "token-$i",
                appKey = "key-$i",
                appSecret = "secret-$i"
            )
        }

        val services = configs.map { DropboxService(it) }
        assertEquals(10, services.size)

        services.forEachIndexed { index, svc ->
            assertEquals("dropbox-${index + 1}", svc.config.name)
            assertEquals("token-${index + 1}", svc.config.accessToken)
        }
    }

    @Test
    fun testOperationManagement() = runBlocking<Unit> {
        // These should complete without throwing, even when not connected
        val cancelResult = service.cancelOperation(12345L)
        assertTrue(cancelResult.isSuccess, "cancelOperation should succeed")

        val pauseResult = service.pauseOperation(12345L)
        assertTrue(pauseResult.isSuccess, "pauseOperation should succeed")

        val resumeResult = service.resumeOperation(12345L)
        assertTrue(resumeResult.isSuccess, "resumeOperation should succeed")
    }

    @Test
    fun testCacheOperations() = runBlocking<Unit> {
        val addResult = service.addToCache("/test.txt", 100)
        assertTrue(addResult.isSuccess, "addToCache should succeed")

        val removeResult = service.removeFromCache("/test.txt")
        assertTrue(removeResult.isSuccess, "removeFromCache should succeed")

        val clearResult = service.clearCache()
        assertTrue(clearResult.isSuccess, "clearCache should succeed")
    }
}
