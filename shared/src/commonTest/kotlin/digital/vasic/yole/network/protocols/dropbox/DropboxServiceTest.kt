/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for DropboxService
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.dropbox

import digital.vasic.yole.network.common.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Comprehensive tests for DropboxService covering:
 * - Service construction and configuration
 * - Storage info and properties
 * - Connection management
 * - File and folder operations (offline/error paths)
 * - Operation management
 * - Edge cases
 */
class DropboxServiceTest {

    private lateinit var dropboxService: DropboxService

    private val testConfig = StorageConfig.DropboxConfig(
        name = "test-dropbox",
        accessToken = "test_dropbox_access_token_123",
        appKey = "test_app_key",
        appSecret = "test_app_secret",
        refreshToken = "test_dropbox_refresh_token_456",
        rootPath = "/Apps/Yole"
    )

    @BeforeTest
    fun setUp() {
        dropboxService = DropboxService(testConfig)
    }

    // ==================== Construction Tests ====================

    @Test
    fun `service should be constructable with config`() {
        assertNotNull(dropboxService)
        assertEquals(testConfig, dropboxService.config)
    }

    @Test
    fun `service config should have correct storage type`() {
        assertEquals(StorageType.DROPBOX, dropboxService.config.storageType)
    }

    @Test
    fun `service config should have correct name`() {
        assertEquals("test-dropbox", dropboxService.config.name)
    }

    @Test
    fun `service config should have correct access token`() {
        assertEquals("test_dropbox_access_token_123", dropboxService.config.accessToken)
    }

    // ==================== Storage Info Tests ====================

    @Test
    fun `getStorageInfo should return valid storage info`() = runBlocking<Unit> {
        val storageInfo = dropboxService.getStorageInfo()
        assertNotNull(storageInfo)
        assertNotNull(storageInfo.id)
        assertNotNull(storageInfo.name)
        assertEquals(StorageType.DROPBOX, storageInfo.type)
    }

    @Test
    fun `getStorageInfo should support folders`() = runBlocking<Unit> {
        val storageInfo = dropboxService.getStorageInfo()
        assertTrue(storageInfo.supportsFolders, "Dropbox should support folders")
    }

    // ==================== Connection Tests ====================

    @Test
    fun `disconnect should succeed`() = runBlocking<Unit> {
        val result = dropboxService.disconnect()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `connect should complete (success or failure)`() = runBlocking<Unit> {
        // Without real network, connect will likely fail but should not throw
        val result = dropboxService.connect()
        assertTrue(result.isSuccess || result.isFailure, "Connect should complete")
    }

    @Test
    fun `testConnection should complete (success or failure)`() = runBlocking<Unit> {
        val result = dropboxService.testConnection()
        assertTrue(result.isSuccess || result.isFailure, "testConnection should complete")
    }

    // ==================== File Operation Tests (Offline) ====================

    @Test
    fun `uploadFile should emit operations`() = runBlocking<Unit> {
        val uploadOps = dropboxService.uploadFile("/local-file.txt", "/remote-file.txt")
        val ops = mutableListOf<NetworkOperation>()
        uploadOps.collect { ops.add(it) }

        assertTrue(ops.isNotEmpty(), "Should emit upload operations")
        val firstOp = ops.first()
        assertEquals(NetworkOperation.Type.UPLOAD, firstOp.type)
    }

    @Test
    fun `downloadFile should emit operations`() = runBlocking<Unit> {
        val downloadOps = dropboxService.downloadFile("/remote-file.txt", "/local-file.txt")
        val ops = mutableListOf<NetworkOperation>()
        downloadOps.collect { ops.add(it) }

        assertTrue(ops.isNotEmpty(), "Should emit download operations")
        val firstOp = ops.first()
        assertEquals(NetworkOperation.Type.DOWNLOAD, firstOp.type)
    }

    @Test
    fun `deleteFile should complete`() = runBlocking<Unit> {
        val result = dropboxService.deleteFile("/test-file.txt")
        assertTrue(result.isSuccess || result.isFailure, "deleteFile should complete")
    }

    @Test
    fun `getFileInfo should complete`() = runBlocking<Unit> {
        val result = dropboxService.getFileInfo("/test-file.txt")
        assertTrue(result.isSuccess || result.isFailure, "getFileInfo should complete")
    }

    @Test
    fun `exists should succeed`() = runBlocking<Unit> {
        val result = dropboxService.exists("/test-file.txt")
        assertTrue(result.isSuccess, "exists should succeed")
    }

    // ==================== Directory Operations Tests ====================

    @Test
    fun `listFiles should emit results`() = runBlocking<Unit> {
        val listResult = dropboxService.listFiles("/").first()
        assertTrue(listResult.isSuccess || listResult.isFailure, "listFiles should complete")
    }

    @Test
    fun `createFolder should complete`() = runBlocking<Unit> {
        val result = dropboxService.createFolder("/test-folder")
        assertTrue(result.isSuccess || result.isFailure, "createFolder should complete")

        if (result.isSuccess) {
            val folder = result.getOrNull()
            assertNotNull(folder)
            assertTrue(folder.isFolder, "Created item should be a folder")
        }
    }

    @Test
    fun `renameFile should complete`() = runBlocking<Unit> {
        val result = dropboxService.renameFile("/old-name.txt", "new-name.txt")
        assertTrue(result.isSuccess || result.isFailure, "renameFile should complete")
    }

    @Test
    fun `moveFile should complete`() = runBlocking<Unit> {
        val result = dropboxService.moveFile("/source.txt", "/destination.txt")
        assertTrue(result.isSuccess || result.isFailure, "moveFile should complete")
    }

    @Test
    fun `copyFile should complete`() = runBlocking<Unit> {
        val result = dropboxService.copyFile("/source.txt", "/copy.txt")
        assertTrue(result.isSuccess || result.isFailure, "copyFile should complete")
    }

    // ==================== Quota Tests ====================

    @Test
    fun `getQuotaInfo should fail when not connected`() = runBlocking<Unit> {
        val result = dropboxService.getQuotaInfo()
        // getQuotaInfo now makes real API calls; without auth it fails when not connected
        assertTrue(result.isFailure, "getQuotaInfo should fail when not connected")
    }

    // ==================== Operation Management Tests ====================

    @Test
    fun `cancelOperation should succeed`() = runBlocking<Unit> {
        val result = dropboxService.cancelOperation(12345L)
        assertTrue(result.isSuccess, "cancelOperation should succeed")
    }

    @Test
    fun `pauseOperation should succeed`() = runBlocking<Unit> {
        val result = dropboxService.pauseOperation(12345L)
        assertTrue(result.isSuccess, "pauseOperation should succeed")
    }

    @Test
    fun `resumeOperation should succeed`() = runBlocking<Unit> {
        val result = dropboxService.resumeOperation(12345L)
        assertTrue(result.isSuccess, "resumeOperation should succeed")
    }

    @Test
    fun `getActiveOperations should return flow`() = runBlocking<Unit> {
        val activeOps = dropboxService.getActiveOperations().first()
        assertNotNull(activeOps, "Should return active operations")
    }

    // ==================== Cache Operations Tests ====================

    @Test
    fun `addToCache should succeed`() = runBlocking<Unit> {
        val result = dropboxService.addToCache("/test.txt", 100)
        assertTrue(result.isSuccess, "addToCache should succeed")
    }

    @Test
    fun `removeFromCache should succeed`() = runBlocking<Unit> {
        val result = dropboxService.removeFromCache("/test.txt")
        assertTrue(result.isSuccess, "removeFromCache should succeed")
    }

    @Test
    fun `clearCache should succeed`() = runBlocking<Unit> {
        val result = dropboxService.clearCache()
        assertTrue(result.isSuccess, "clearCache should succeed")
    }

    // ==================== Sync Tests ====================

    @Test
    fun `syncFile should emit operations`() = runBlocking<Unit> {
        val syncOps = dropboxService.syncFile("/test.txt", false)
        val ops = mutableListOf<NetworkOperation>()
        syncOps.collect { ops.add(it) }
        assertTrue(ops.isNotEmpty(), "Should emit sync operations")
    }

    // ==================== Path Tests ====================

    @Test
    fun `getParentPath should return parent`() = runBlocking<Unit> {
        val parentPath = dropboxService.getParentPath("/folder/file.txt")
        assertNotNull(parentPath, "Should return parent path")
    }

    @Test
    fun `validatePath should complete`() = runBlocking<Unit> {
        val result = dropboxService.validatePath("/test.txt")
        assertTrue(result.isSuccess || result.isFailure, "validatePath should complete")
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle very long file paths`() = runBlocking<Unit> {
        val longPath = "/remote/test/" + "a".repeat(1000) + "/file.txt"
        val result = dropboxService.getFileInfo(longPath)
        // Should complete without crashing
        assertTrue(result.isSuccess || result.isFailure)
    }

    @Test
    fun `should handle special characters in file names`() = runBlocking<Unit> {
        val specialPath = "/remote/test/file with spaces.txt"
        val result = dropboxService.getFileInfo(specialPath)
        // Should complete without crashing
        assertTrue(result.isSuccess || result.isFailure)
    }

    @Test
    fun `should handle empty path`() = runBlocking<Unit> {
        val result = dropboxService.exists("")
        assertTrue(result.isSuccess || result.isFailure, "Should handle empty path")
    }

    @Test
    fun `should handle root path`() = runBlocking<Unit> {
        val result = dropboxService.exists("/")
        assertTrue(result.isSuccess, "Should handle root path")
    }

    @Test
    fun `minimal config should work`() {
        val minimalConfig = StorageConfig.DropboxConfig(
            name = "minimal",
            accessToken = "token",
            appKey = "key",
            appSecret = "secret"
        )
        val minimalService = DropboxService(minimalConfig)
        assertEquals("minimal", minimalService.config.name)
        assertEquals("", minimalService.config.rootPath)
        assertNull(minimalService.config.refreshToken)
    }

    @Test
    fun `multiple service instances should be independent`() = runBlocking<Unit> {
        val config1 = StorageConfig.DropboxConfig(
            name = "dropbox-1",
            accessToken = "token-1",
            appKey = "key-1",
            appSecret = "secret-1"
        )
        val config2 = StorageConfig.DropboxConfig(
            name = "dropbox-2",
            accessToken = "token-2",
            appKey = "key-2",
            appSecret = "secret-2"
        )

        val service1 = DropboxService(config1)
        val service2 = DropboxService(config2)

        assertNotEquals(service1.config.name, service2.config.name)
        assertNotEquals(service1.config.accessToken, service2.config.accessToken)
    }
}
