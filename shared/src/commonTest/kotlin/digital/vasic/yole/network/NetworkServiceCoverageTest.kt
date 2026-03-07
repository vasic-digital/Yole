/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive coverage tests for NetworkStorageService,
 * StorageQuota, NetworkStorageException, and
 * NetworkStorageConfigService uncovered branches.
 *
 *########################################################*/
package digital.vasic.yole.network

import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.config.NetworkStorageConfigService
import digital.vasic.yole.network.config.StorageFeature
import digital.vasic.yole.network.config.StorageTypeInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive coverage tests targeting uncovered branches in:
 * - StorageQuota.formatBytes (all 4 branches for each formatted property)
 * - NetworkStorageService.rootPath default getter
 * - NetworkStorageException.fromThrowable edge cases
 * - NetworkStorageException utility methods (toUserMessage, isRetryable, isPermanentFailure, getSuggestedAction)
 * - NetworkStorageConfigService missed lines
 * - StorageTypeInfo and StorageFeature coverage
 */
class NetworkServiceCoverageTest {

    // ==================== StorageQuota.formatBytes branch coverage ====================

    @Test
    fun testFormatBytesZeroBytes() {
        val quota = StorageQuota(
            totalSpace = 0L, usedSpace = 0L, availableSpace = 0L,
            usagePercentage = 0.0, isFull = false, isLowOnSpace = false
        )
        assertEquals("0B", quota.formattedTotalSpace)
        assertEquals("0B", quota.formattedUsedSpace)
        assertEquals("0B", quota.formattedAvailableSpace)
    }

    @Test
    fun testFormatBytesExactly1023Bytes() {
        val quota = StorageQuota(
            totalSpace = 1023L, usedSpace = 1023L, availableSpace = 1023L,
            usagePercentage = 1.0, isFull = false, isLowOnSpace = false
        )
        assertEquals("1023B", quota.formattedTotalSpace)
        assertEquals("1023B", quota.formattedUsedSpace)
        assertEquals("1023B", quota.formattedAvailableSpace)
    }

    @Test
    fun testFormatBytesExactly1024BytesBoundary() {
        val quota = StorageQuota(
            totalSpace = 1024L, usedSpace = 1024L, availableSpace = 1024L,
            usagePercentage = 1.0, isFull = false, isLowOnSpace = false
        )
        assertEquals("1KB", quota.formattedTotalSpace)
        assertEquals("1KB", quota.formattedUsedSpace)
        assertEquals("1KB", quota.formattedAvailableSpace)
    }

    @Test
    fun testFormatBytesKiloByteBoundaryMinus1() {
        val quota = StorageQuota(
            totalSpace = 1024L * 1024 - 1, usedSpace = 1024L * 1024 - 1,
            availableSpace = 1024L * 1024 - 1,
            usagePercentage = 1.0, isFull = false, isLowOnSpace = false
        )
        // 1048575 / 1024 = 1023
        assertEquals("1023KB", quota.formattedTotalSpace)
    }

    @Test
    fun testFormatBytesExactlyOneMegabyte() {
        val quota = StorageQuota(
            totalSpace = 1024L * 1024, usedSpace = 1024L * 1024,
            availableSpace = 1024L * 1024,
            usagePercentage = 1.0, isFull = false, isLowOnSpace = false
        )
        assertEquals("1MB", quota.formattedTotalSpace)
        assertEquals("1MB", quota.formattedUsedSpace)
        assertEquals("1MB", quota.formattedAvailableSpace)
    }

    @Test
    fun testFormatBytesMegabyteBoundaryMinus1() {
        val quota = StorageQuota(
            totalSpace = 1024L * 1024 * 1024 - 1,
            usedSpace = 1024L * 1024 * 1024 - 1,
            availableSpace = 1024L * 1024 * 1024 - 1,
            usagePercentage = 1.0, isFull = false, isLowOnSpace = false
        )
        // (1073741823) / (1024*1024) = 1023
        assertEquals("1023MB", quota.formattedTotalSpace)
    }

    @Test
    fun testFormatBytesExactlyOneGigabyte() {
        val quota = StorageQuota(
            totalSpace = 1024L * 1024 * 1024,
            usedSpace = 1024L * 1024 * 1024,
            availableSpace = 1024L * 1024 * 1024,
            usagePercentage = 1.0, isFull = false, isLowOnSpace = false
        )
        assertEquals("1GB", quota.formattedTotalSpace)
        assertEquals("1GB", quota.formattedUsedSpace)
        assertEquals("1GB", quota.formattedAvailableSpace)
    }

    @Test
    fun testFormatBytesLargeGigabytes() {
        val quota = StorageQuota(
            totalSpace = 100L * 1024 * 1024 * 1024,
            usedSpace = 50L * 1024 * 1024 * 1024,
            availableSpace = 50L * 1024 * 1024 * 1024,
            usagePercentage = 0.5, isFull = false, isLowOnSpace = false
        )
        assertEquals("100GB", quota.formattedTotalSpace)
        assertEquals("50GB", quota.formattedUsedSpace)
        assertEquals("50GB", quota.formattedAvailableSpace)
    }

    @Test
    fun testFormattedUsedSpaceBytesRange() {
        val quota = StorageQuota(
            totalSpace = 10000L, usedSpace = 500L, availableSpace = 9500L,
            usagePercentage = 0.05, isFull = false, isLowOnSpace = false
        )
        assertEquals("500B", quota.formattedUsedSpace)
    }

    @Test
    fun testFormattedAvailableSpaceBytesRange() {
        val quota = StorageQuota(
            totalSpace = 10000L, usedSpace = 9900L, availableSpace = 100L,
            usagePercentage = 0.99, isFull = false, isLowOnSpace = true
        )
        assertEquals("100B", quota.formattedAvailableSpace)
    }

    @Test
    fun testFormattedUsedSpaceGigabyteRange() {
        val quota = StorageQuota(
            totalSpace = 10L * 1024 * 1024 * 1024,
            usedSpace = 5L * 1024 * 1024 * 1024,
            availableSpace = 5L * 1024 * 1024 * 1024,
            usagePercentage = 0.5, isFull = false, isLowOnSpace = false
        )
        assertEquals("5GB", quota.formattedUsedSpace)
    }

    @Test
    fun testFormattedAvailableSpaceMegabyteRange() {
        val quota = StorageQuota(
            totalSpace = 10L * 1024 * 1024 * 1024,
            usedSpace = 10L * 1024 * 1024 * 1024 - 10L * 1024 * 1024,
            availableSpace = 10L * 1024 * 1024,
            usagePercentage = 0.99, isFull = false, isLowOnSpace = true
        )
        assertEquals("10MB", quota.formattedAvailableSpace)
    }

    @Test
    fun testUsagePercentageStringRounding() {
        val quota = StorageQuota(
            totalSpace = 1000L, usedSpace = 333L, availableSpace = 667L,
            usagePercentage = 0.333, isFull = false, isLowOnSpace = false
        )
        assertEquals("33%", quota.usagePercentageString)
    }

    @Test
    fun testUsagePercentageStringHighPrecision() {
        val quota = StorageQuota(
            totalSpace = 1000L, usedSpace = 999L, availableSpace = 1L,
            usagePercentage = 0.999, isFull = false, isLowOnSpace = true
        )
        assertEquals("99%", quota.usagePercentageString)
    }

    // ==================== NetworkStorageService.rootPath default getter ====================

    @Test
    fun testDefaultRootPathReturnsSlash() {
        // Create a minimal implementation that uses the default rootPath getter
        val service = object : NetworkStorageService {
            override val config: StorageConfig = StorageConfig.WebDavConfig(
                name = "test", url = "https://example.com",
                username = "user", password = "pass"
            )
            override suspend fun connect() = Result.success(Unit)
            override suspend fun disconnect() = Result.success(Unit)
            override val isOnline: Boolean = false
            override fun listFiles(path: String): Flow<Result<List<NetworkDocument>>> = flowOf()
            override suspend fun downloadFile(remotePath: String, localPath: String): Flow<NetworkOperation> = flow {}
            override suspend fun uploadFile(localPath: String, remotePath: String): Flow<NetworkOperation> = flow {}
            override suspend fun deleteFile(remotePath: String) = Result.success(Unit)
            override suspend fun createFolder(remotePath: String): Result<NetworkDocument> = Result.failure(Exception("not impl"))
            override suspend fun renameFile(remotePath: String, newName: String) = Result.success(Unit)
            override suspend fun moveFile(sourcePath: String, destinationPath: String): Result<NetworkDocument> = Result.failure(Exception("not impl"))
            override suspend fun copyFile(sourcePath: String, destinationPath: String) = Result.success(Unit)
            override suspend fun getFileInfo(remotePath: String): Result<NetworkDocument> = Result.failure(Exception("not impl"))
            override fun getActiveOperations(): Flow<List<NetworkOperation>> = flowOf(emptyList())
            override suspend fun cancelOperation(operationId: Long) = Result.success(Unit)
            override suspend fun pauseOperation(operationId: Long) = Result.success(Unit)
            override suspend fun resumeOperation(operationId: Long) = Result.success(Unit)
            override suspend fun getStorageInfo(): NetworkStorage = NetworkStorage.mock()
            override suspend fun testConnection() = Result.success(true)
            override fun getCacheEntries(path: String?): Flow<List<CacheEntry>> = flowOf(emptyList())
            override suspend fun addToCache(remotePath: String, priority: Int) = Result.success(Unit)
            override suspend fun removeFromCache(remotePath: String) = Result.success(Unit)
            override suspend fun clearCache() = Result.success(Unit)
            override fun getSyncStatus(path: String?): Flow<Map<String, SyncStatus>> = flowOf(emptyMap())
            override suspend fun syncFile(remotePath: String, forceSync: Boolean): Flow<NetworkOperation> = flow {}
            override suspend fun syncAll(forceSync: Boolean): Flow<NetworkOperation> = flow {}
            override fun searchFiles(query: String, path: String?, includeContent: Boolean): Flow<Result<List<NetworkDocument>>> = flowOf()
            override fun getRecentChanges(since: kotlinx.datetime.Instant, path: String?): Flow<List<NetworkDocument>> = flowOf()
            override suspend fun getQuotaInfo(): Result<StorageQuota> = Result.failure(Exception("not impl"))
            override suspend fun exists(remotePath: String) = Result.success(false)
            override fun getParentPath(remotePath: String): String? = null
            override fun validatePath(remotePath: String) = Result.success(Unit)
            // NOTE: rootPath not overridden, uses default from interface
        }

        assertEquals("/", service.rootPath, "Default rootPath from interface should be /")
    }

    @Test
    fun testOverriddenRootPath() {
        val service = object : NetworkStorageService {
            override val config: StorageConfig = StorageConfig.FtpConfig(
                name = "test", host = "ftp.example.com",
                username = "user", password = "pass"
            )
            override suspend fun connect() = Result.success(Unit)
            override suspend fun disconnect() = Result.success(Unit)
            override val isOnline: Boolean = false
            override fun listFiles(path: String): Flow<Result<List<NetworkDocument>>> = flowOf()
            override suspend fun downloadFile(remotePath: String, localPath: String): Flow<NetworkOperation> = flow {}
            override suspend fun uploadFile(localPath: String, remotePath: String): Flow<NetworkOperation> = flow {}
            override suspend fun deleteFile(remotePath: String) = Result.success(Unit)
            override suspend fun createFolder(remotePath: String): Result<NetworkDocument> = Result.failure(Exception("not impl"))
            override suspend fun renameFile(remotePath: String, newName: String) = Result.success(Unit)
            override suspend fun moveFile(sourcePath: String, destinationPath: String): Result<NetworkDocument> = Result.failure(Exception("not impl"))
            override suspend fun copyFile(sourcePath: String, destinationPath: String) = Result.success(Unit)
            override suspend fun getFileInfo(remotePath: String): Result<NetworkDocument> = Result.failure(Exception("not impl"))
            override fun getActiveOperations(): Flow<List<NetworkOperation>> = flowOf(emptyList())
            override suspend fun cancelOperation(operationId: Long) = Result.success(Unit)
            override suspend fun pauseOperation(operationId: Long) = Result.success(Unit)
            override suspend fun resumeOperation(operationId: Long) = Result.success(Unit)
            override suspend fun getStorageInfo(): NetworkStorage = NetworkStorage.mock()
            override suspend fun testConnection() = Result.success(true)
            override fun getCacheEntries(path: String?): Flow<List<CacheEntry>> = flowOf(emptyList())
            override suspend fun addToCache(remotePath: String, priority: Int) = Result.success(Unit)
            override suspend fun removeFromCache(remotePath: String) = Result.success(Unit)
            override suspend fun clearCache() = Result.success(Unit)
            override fun getSyncStatus(path: String?): Flow<Map<String, SyncStatus>> = flowOf(emptyMap())
            override suspend fun syncFile(remotePath: String, forceSync: Boolean): Flow<NetworkOperation> = flow {}
            override suspend fun syncAll(forceSync: Boolean): Flow<NetworkOperation> = flow {}
            override fun searchFiles(query: String, path: String?, includeContent: Boolean): Flow<Result<List<NetworkDocument>>> = flowOf()
            override fun getRecentChanges(since: kotlinx.datetime.Instant, path: String?): Flow<List<NetworkDocument>> = flowOf()
            override suspend fun getQuotaInfo(): Result<StorageQuota> = Result.failure(Exception("not impl"))
            override suspend fun exists(remotePath: String) = Result.success(false)
            override fun getParentPath(remotePath: String): String? = null
            override fun validatePath(remotePath: String) = Result.success(Unit)
            override val rootPath: String = "/custom/root"
        }

        assertEquals("/custom/root", service.rootPath, "Overridden rootPath should be /custom/root")
    }

    // ==================== NetworkStorageException.fromThrowable edge cases ====================

    @Test
    fun testFromThrowableWithNullMessage() {
        // Exception with null message should fall through to GenericError
        val cause = object : Throwable(null as String?) {}
        val ex = NetworkStorageException.fromThrowable(cause, operation = "test")
        assertTrue(ex is NetworkStorageException.GenericError)
        assertEquals("Unknown error", ex.message)
    }

    @Test
    fun testFromThrowableWithEmptyMessage() {
        val cause = RuntimeException("")
        val ex = NetworkStorageException.fromThrowable(cause)
        // Empty string does not contain "timeout" etc., falls to GenericError
        assertTrue(ex is NetworkStorageException.GenericError)
    }

    @Test
    fun testFromThrowablePreservesFilePath() {
        val cause = RuntimeException("Permission issue")
        val ex = NetworkStorageException.fromThrowable(
            cause,
            filePath = "/specific/file.txt"
        )
        assertTrue(ex is NetworkStorageException.FileOperationException.PermissionDenied)
        assertEquals("/specific/file.txt", (ex as NetworkStorageException.FileOperationException.PermissionDenied).filePath)
    }

    @Test
    fun testFromThrowablePermissionWithoutFilePath() {
        val cause = RuntimeException("Permission denied")
        val ex = NetworkStorageException.fromThrowable(cause)
        assertTrue(ex is NetworkStorageException.FileOperationException.PermissionDenied)
        assertEquals("unknown", (ex as NetworkStorageException.FileOperationException.PermissionDenied).filePath)
    }

    @Test
    fun testFromThrowableGenericWithAllParameters() {
        val cause = RuntimeException("unrecognized error")
        val ex = NetworkStorageException.fromThrowable(
            cause,
            operation = "sync",
            filePath = "/path/to/file",
            remotePath = "/remote/path"
        )
        assertTrue(ex is NetworkStorageException.GenericError)
        val generic = ex as NetworkStorageException.GenericError
        assertEquals("sync", generic.operation)
        assertEquals("/path/to/file", generic.context["filePath"])
        assertEquals("/remote/path", generic.context["remotePath"])
    }

    @Test
    fun testFromThrowableTimeoutCaseInsensitive() {
        val cause = RuntimeException("Connection TIMEOUT occurred")
        val ex = NetworkStorageException.fromThrowable(cause)
        assertTrue(ex is NetworkStorageException.ConnectionException.Timeout)
    }

    @Test
    fun testFromThrowableAuthenticationCaseInsensitive() {
        val cause = RuntimeException("AUTHENTICATION required")
        val ex = NetworkStorageException.fromThrowable(cause)
        assertTrue(ex is NetworkStorageException.ConnectionException.Authentication)
    }

    @Test
    fun testFromThrowableQuotaCaseInsensitive() {
        val cause = RuntimeException("QUOTA limit reached")
        val ex = NetworkStorageException.fromThrowable(cause)
        assertTrue(ex is NetworkStorageException.QuotaException.Exceeded)
    }

    // ==================== NetworkStorageException utility method coverage ====================

    @Test
    fun testToUserMessageForConnectionFailed() {
        val ex = NetworkStorageException.ConnectionException.Failed()
        val msg = ex.toUserMessage()
        // Falls into the else branch since ConnectionException.Failed is not specifically mapped
        assertEquals("Connection failed", msg)
    }

    @Test
    fun testToUserMessageForNotConnected() {
        val ex = NetworkStorageException.ConnectionException.NotConnected()
        val msg = ex.toUserMessage()
        // Falls into the else branch
        assertEquals("Not connected", msg)
    }

    @Test
    fun testToUserMessageForListFailed() {
        val ex = NetworkStorageException.FileOperationException.ListFailed(path = "/test")
        val msg = ex.toUserMessage()
        // Falls into else branch
        assertTrue(msg.contains("List files failed"))
    }

    @Test
    fun testToUserMessageForUploadFailed() {
        val ex = NetworkStorageException.FileOperationException.UploadFailed(path = "/upload.txt")
        val msg = ex.toUserMessage()
        assertTrue(msg.contains("Upload failed"))
    }

    @Test
    fun testToUserMessageForDownloadFailed() {
        val ex = NetworkStorageException.FileOperationException.DownloadFailed(path = "/download.txt")
        val msg = ex.toUserMessage()
        assertTrue(msg.contains("Download failed"))
    }

    @Test
    fun testToUserMessageForVersionMismatch() {
        val ex = NetworkStorageException.ProtocolException.VersionMismatch(
            protocol = "smb",
            expectedVersion = "3.0",
            actualVersion = "2.0"
        )
        val msg = ex.toUserMessage()
        // Falls into else branch
        assertTrue(msg.contains("smb"))
    }

    @Test
    fun testToUserMessageForConfigurationError() {
        val ex = NetworkStorageException.ProtocolException.ConfigurationError(
            protocol = "webdav",
            configKey = "url",
            configValue = null,
            expectedType = "String"
        )
        val msg = ex.toUserMessage()
        assertTrue(msg.contains("webdav"))
    }

    @Test
    fun testToUserMessageForSyncInterrupted() {
        val ex = NetworkStorageException.SyncException.Interrupted(
            remotePath = "/doc.txt",
            operation = "download",
            bytesProcessed = 500L,
            totalBytes = 1000L
        )
        val msg = ex.toUserMessage()
        assertTrue(msg.contains("Sync interrupted"))
    }

    @Test
    fun testToUserMessageForRetryLimitExceeded() {
        val ex = NetworkStorageException.SyncException.RetryLimitExceeded(
            remotePath = "/retry.txt",
            retryCount = 3,
            maxRetries = 3
        )
        val msg = ex.toUserMessage()
        assertTrue(msg.contains("retry limit"))
    }

    @Test
    fun testIsRetryableForConnectionFailed() {
        val ex = NetworkStorageException.ConnectionException.Failed()
        assertFalse(ex.isRetryable())
    }

    @Test
    fun testIsRetryableForNotConnected() {
        val ex = NetworkStorageException.ConnectionException.NotConnected()
        assertFalse(ex.isRetryable())
    }

    @Test
    fun testIsRetryableForSslError() {
        val ex = NetworkStorageException.ConnectionException.SslError()
        assertFalse(ex.isRetryable())
    }

    @Test
    fun testIsRetryableForGenericError() {
        val ex = NetworkStorageException.GenericError(message = "something")
        assertFalse(ex.isRetryable())
    }

    @Test
    fun testIsPermanentFailureForConnectionFailed() {
        val ex = NetworkStorageException.ConnectionException.Failed()
        assertFalse(ex.isPermanentFailure())
    }

    @Test
    fun testIsPermanentFailureForNetworkUnavailable() {
        val ex = NetworkStorageException.ConnectionException.NetworkUnavailable()
        assertFalse(ex.isPermanentFailure())
    }

    @Test
    fun testIsPermanentFailureForGenericError() {
        val ex = NetworkStorageException.GenericError(message = "something")
        assertFalse(ex.isPermanentFailure())
    }

    @Test
    fun testIsPermanentFailureForSyncConflict() {
        val now = Clock.System.now()
        val ex = NetworkStorageException.SyncException.Conflict(
            remotePath = "/x",
            conflictType = NetworkStorageException.ConflictType.UNKNOWN,
            remoteTimestamp = now,
            localTimestamp = now
        )
        assertFalse(ex.isPermanentFailure())
    }

    @Test
    fun testGetSuggestedActionForServerUnreachable() {
        val ex = NetworkStorageException.ConnectionException.ServerUnreachable(
            host = "example.com", port = 443
        )
        // ServerUnreachable is not specifically mapped in getSuggestedAction, returns null
        assertNull(ex.getSuggestedAction())
    }

    @Test
    fun testGetSuggestedActionForNetworkUnavailable() {
        val ex = NetworkStorageException.ConnectionException.NetworkUnavailable()
        assertNull(ex.getSuggestedAction())
    }

    @Test
    fun testGetSuggestedActionForBandwidthExceeded() {
        val ex = NetworkStorageException.QuotaException.BandwidthExceeded(
            usedBandwidth = 100L, totalBandwidth = 100L,
            resetTime = Clock.System.now()
        )
        assertNull(ex.getSuggestedAction())
    }

    // ==================== NetworkStorageException additional creation tests ====================

    @Test
    fun testConnectionTimeoutWithCustomMessage() {
        val ex = NetworkStorageException.ConnectionException.Timeout(
            message = "Custom timeout",
            timeoutMs = 60000L
        )
        assertTrue(ex.message!!.contains("60000"))
        assertTrue(ex.message!!.contains("Custom timeout"))
        assertEquals("CONNECTION_TIMEOUT", ex.errorCode)
    }

    @Test
    fun testGenericErrorWithCustomErrorCode() {
        val ex = NetworkStorageException.GenericError(
            message = "Custom error",
            errorCode = "CUSTOM_CODE"
        )
        assertEquals("CUSTOM_CODE", ex.errorCode)
    }

    @Test
    fun testSyncConflictAllConflictTypes() {
        val now = Clock.System.now()
        NetworkStorageException.ConflictType.entries.forEach { conflictType ->
            val ex = NetworkStorageException.SyncException.Conflict(
                remotePath = "/file.txt",
                conflictType = conflictType,
                remoteTimestamp = now,
                localTimestamp = now
            )
            assertTrue(ex.message!!.contains(conflictType.name))
            assertEquals("SYNC_CONFLICT", ex.errorCode)
        }
    }

    @Test
    fun testFileOperationExceptionInfoFailedWithCustomMessage() {
        val ex = NetworkStorageException.FileOperationException.InfoFailed(
            message = "Cannot get info",
            path = "/info.txt"
        )
        assertTrue(ex.message!!.contains("Cannot get info"))
        assertTrue(ex.message!!.contains("/info.txt"))
    }

    @Test
    fun testCacheCorruptionAllFlags() {
        val ex = NetworkStorageException.CacheException.Corruption(
            cachePath = "/cache",
            checksumMismatch = true,
            expectedSize = 2048L,
            actualSize = 1024L
        )
        assertTrue(ex.message!!.contains("checksum mismatch"))
        assertTrue(ex.message!!.contains("2KB"))
        assertTrue(ex.message!!.contains("1KB"))
    }

    // ==================== NetworkStorageConfigService additional coverage ====================

    @Test
    fun testConfigServiceGetSupportedStorageTypesReturnsDistinctTypes() {
        val service = NetworkStorageConfigService()
        val types = service.getSupportedStorageTypes()
        val distinctTypes = types.map { it.type }.toSet()
        assertEquals(types.size, distinctTypes.size, "All types should be distinct")
    }

    @Test
    fun testConfigServiceGetSupportedStorageTypesWebDavHasSharePoint() {
        val service = NetworkStorageConfigService()
        val types = service.getSupportedStorageTypes()
        val webdav = types.find { it.type == StorageType.WEBDAV }
        assertNotNull(webdav)
        assertTrue(webdav.popularServices.contains("SharePoint"))
        assertTrue(webdav.popularServices.contains("Seafile"))
    }

    @Test
    fun testConfigServiceGetSupportedStorageTypesFtpPopularServices() {
        val service = NetworkStorageConfigService()
        val types = service.getSupportedStorageTypes()
        val ftp = types.find { it.type == StorageType.FTP }
        assertNotNull(ftp)
        assertTrue(ftp.popularServices.contains("FileZilla Server"))
        assertTrue(ftp.popularServices.contains("vsftpd"))
        assertTrue(ftp.popularServices.contains("ProFTPD"))
    }

    @Test
    fun testConfigServiceGetSupportedStorageTypesSftpPopularServices() {
        val service = NetworkStorageConfigService()
        val types = service.getSupportedStorageTypes()
        val sftp = types.find { it.type == StorageType.SFTP }
        assertNotNull(sftp)
        assertTrue(sftp.popularServices.contains("OpenSSH"))
    }

    @Test
    fun testConfigServiceGetSupportedStorageTypesSmbPopularServices() {
        val service = NetworkStorageConfigService()
        val types = service.getSupportedStorageTypes()
        val smb = types.find { it.type == StorageType.SMB }
        assertNotNull(smb)
        assertTrue(smb.popularServices.contains("Windows File Sharing"))
        assertTrue(smb.popularServices.contains("Samba"))
        assertTrue(smb.popularServices.contains("NAS devices"))
    }

    @Test
    fun testConfigServiceGetSupportedStorageTypesGDrivePopularServices() {
        val service = NetworkStorageConfigService()
        val types = service.getSupportedStorageTypes()
        val gdrive = types.find { it.type == StorageType.GOOGLE_DRIVE }
        assertNotNull(gdrive)
        assertTrue(gdrive.popularServices.contains("Google Drive"))
        assertTrue(gdrive.popularServices.contains("Google Workspace"))
    }

    @Test
    fun testConfigServiceGetSupportedStorageTypesDropboxPopularServices() {
        val service = NetworkStorageConfigService()
        val types = service.getSupportedStorageTypes()
        val dropbox = types.find { it.type == StorageType.DROPBOX }
        assertNotNull(dropbox)
        assertTrue(dropbox.popularServices.contains("Dropbox"))
        assertTrue(dropbox.popularServices.contains("Dropbox Business"))
    }

    @Test
    fun testConfigServiceGetSupportedStorageTypesOneDrivePopularServices() {
        val service = NetworkStorageConfigService()
        val types = service.getSupportedStorageTypes()
        val onedrive = types.find { it.type == StorageType.ONEDRIVE }
        assertNotNull(onedrive)
        assertTrue(onedrive.popularServices.contains("OneDrive"))
        assertTrue(onedrive.popularServices.contains("SharePoint Online"))
    }

    @Test
    fun testConfigServiceGetSupportedStorageTypesGitPopularServices() {
        val service = NetworkStorageConfigService()
        val types = service.getSupportedStorageTypes()
        val git = types.find { it.type == StorageType.GIT }
        assertNotNull(git)
        assertTrue(git.popularServices.contains("Bitbucket"))
        assertTrue(git.popularServices.contains("Gitea"))
    }

    @Test
    fun testConfigServiceOneDriveHasVersioningFeature() {
        val service = NetworkStorageConfigService()
        val types = service.getSupportedStorageTypes()
        val onedrive = types.find { it.type == StorageType.ONEDRIVE }
        assertNotNull(onedrive)
        assertTrue(onedrive.supportedFeatures.contains(StorageFeature.VERSIONING))
    }

    @Test
    fun testConfigServiceDropboxHasFoldersFeature() {
        val service = NetworkStorageConfigService()
        val types = service.getSupportedStorageTypes()
        val dropbox = types.find { it.type == StorageType.DROPBOX }
        assertNotNull(dropbox)
        assertTrue(dropbox.supportedFeatures.contains(StorageFeature.FOLDERS))
    }

    // ==================== StorageQuota data class operations ====================

    @Test
    fun testStorageQuotaToString() {
        val quota = StorageQuota(
            totalSpace = 1024L, usedSpace = 512L, availableSpace = 512L,
            usagePercentage = 0.5, isFull = false, isLowOnSpace = false
        )
        val str = quota.toString()
        assertTrue(str.contains("totalSpace=1024"))
        assertTrue(str.contains("usedSpace=512"))
    }

    @Test
    fun testStorageQuotaCopyWithMetadata() {
        val original = StorageQuota(
            totalSpace = 1000L, usedSpace = 500L, availableSpace = 500L,
            usagePercentage = 0.5, isFull = false, isLowOnSpace = false
        )
        val withMeta = original.copy(
            metadata = mapOf("tier" to "premium", "region" to "eu-west")
        )
        assertEquals(2, withMeta.metadata.size)
        assertEquals("premium", withMeta.metadata["tier"])
        assertEquals("eu-west", withMeta.metadata["region"])
        // Original unchanged
        assertTrue(original.metadata.isEmpty())
    }

    @Test
    fun testStorageQuotaWithExpiresAtCopy() {
        val now = Clock.System.now()
        val quota = StorageQuota(
            totalSpace = 100L, usedSpace = 50L, availableSpace = 50L,
            usagePercentage = 0.5, isFull = false, isLowOnSpace = false,
            expiresAt = now
        )
        val cleared = quota.copy(expiresAt = null)
        assertNull(cleared.expiresAt)
        assertEquals(now, quota.expiresAt)
    }

    // ==================== StorageTypeInfo data class ====================

    @Test
    fun testStorageTypeInfoToString() {
        val info = StorageTypeInfo(
            type = StorageType.WEBDAV,
            name = "WebDAV",
            description = "Web-based DAV",
            supportedFeatures = setOf(StorageFeature.FOLDERS),
            popularServices = listOf("Nextcloud")
        )
        val str = info.toString()
        assertTrue(str.contains("WebDAV"))
        assertTrue(str.contains("WEBDAV"))
    }

    @Test
    fun testStorageTypeInfoInequality() {
        val info1 = StorageTypeInfo(
            type = StorageType.FTP,
            name = "FTP",
            description = "desc1",
            supportedFeatures = setOf(StorageFeature.FILE_OPERATIONS),
            popularServices = listOf("vsftpd")
        )
        val info2 = StorageTypeInfo(
            type = StorageType.SFTP,
            name = "SFTP",
            description = "desc2",
            supportedFeatures = setOf(StorageFeature.FILE_OPERATIONS),
            popularServices = listOf("OpenSSH")
        )
        assertFalse(info1 == info2)
    }

    // ==================== StorageFeature enum coverage ====================

    @Test
    fun testAllStorageFeatureEntriesAccessible() {
        val features = StorageFeature.entries
        assertEquals(14, features.size)
        assertTrue(features.contains(StorageFeature.FOLDERS))
        assertTrue(features.contains(StorageFeature.FILE_OPERATIONS))
        assertTrue(features.contains(StorageFeature.METADATA))
        assertTrue(features.contains(StorageFeature.VERSIONING))
        assertTrue(features.contains(StorageFeature.ENCRYPTION))
        assertTrue(features.contains(StorageFeature.PERMISSIONS))
        assertTrue(features.contains(StorageFeature.NETWORK_DISCOVERY))
        assertTrue(features.contains(StorageFeature.SEARCH))
        assertTrue(features.contains(StorageFeature.COLLABORATION))
        assertTrue(features.contains(StorageFeature.OFFLINE_SYNC))
        assertTrue(features.contains(StorageFeature.SHARING))
        assertTrue(features.contains(StorageFeature.OFFICE_INTEGRATION))
        assertTrue(features.contains(StorageFeature.SYNC))
        assertTrue(features.contains(StorageFeature.BACKUP))
    }

    @Test
    fun testStorageFeatureNameValues() {
        assertEquals("FOLDERS", StorageFeature.FOLDERS.name)
        assertEquals("FILE_OPERATIONS", StorageFeature.FILE_OPERATIONS.name)
        assertEquals("OFFICE_INTEGRATION", StorageFeature.OFFICE_INTEGRATION.name)
        assertEquals("BACKUP", StorageFeature.BACKUP.name)
    }

    // ==================== ConflictType enum coverage ====================

    @Test
    fun testAllConflictTypeEntriesAccessible() {
        val types = NetworkStorageException.ConflictType.entries
        assertEquals(6, types.size)
        assertTrue(types.contains(NetworkStorageException.ConflictType.BOTH_MODIFIED))
        assertTrue(types.contains(NetworkStorageException.ConflictType.REMOTE_MODIFIED))
        assertTrue(types.contains(NetworkStorageException.ConflictType.LOCAL_MODIFIED))
        assertTrue(types.contains(NetworkStorageException.ConflictType.DELETE_MODIFY_CONFLICT))
        assertTrue(types.contains(NetworkStorageException.ConflictType.RENAME_CONFLICT))
        assertTrue(types.contains(NetworkStorageException.ConflictType.UNKNOWN))
    }

    @Test
    fun testConflictTypeNameValues() {
        assertEquals("BOTH_MODIFIED", NetworkStorageException.ConflictType.BOTH_MODIFIED.name)
        assertEquals("REMOTE_MODIFIED", NetworkStorageException.ConflictType.REMOTE_MODIFIED.name)
        assertEquals("LOCAL_MODIFIED", NetworkStorageException.ConflictType.LOCAL_MODIFIED.name)
        assertEquals("DELETE_MODIFY_CONFLICT", NetworkStorageException.ConflictType.DELETE_MODIFY_CONFLICT.name)
        assertEquals("RENAME_CONFLICT", NetworkStorageException.ConflictType.RENAME_CONFLICT.name)
        assertEquals("UNKNOWN", NetworkStorageException.ConflictType.UNKNOWN.name)
    }

    // ==================== NetworkStorageException timestamp ====================

    @Test
    fun testExceptionTimestampIsSet() {
        val before = Clock.System.now()
        val ex = NetworkStorageException.ConnectionException.Failed()
        val after = Clock.System.now()
        assertTrue(ex.timestamp >= before)
        assertTrue(ex.timestamp <= after)
    }

    @Test
    fun testExceptionPreservesCause() {
        val originalCause = RuntimeException("root cause")
        val ex = NetworkStorageException.ConnectionException.Failed(cause = originalCause)
        assertEquals(originalCause, ex.cause)
        assertEquals("root cause", ex.cause?.message)
    }

    // ==================== ConfigService refreshConnectionStatus ====================

    @Test
    fun testRefreshConnectionStatusEmptyResult() = runTest {
        val service = NetworkStorageConfigService()
        val result = service.refreshConnectionStatus()
        assertTrue(result.isSuccess)
        assertTrue(service.connectionStatus.value.isEmpty())
    }

    @Test
    fun testRefreshConnectionStatusTwice() = runTest {
        val service = NetworkStorageConfigService()
        val result1 = service.refreshConnectionStatus()
        assertTrue(result1.isSuccess)
        val result2 = service.refreshConnectionStatus()
        assertTrue(result2.isSuccess)
        assertTrue(service.connectionStatus.value.isEmpty())
    }

    // ==================== ConfigService removeStorage and setActiveStorage on empty state ====================

    @Test
    fun testRemoveStorageNonExistentReturnsGenericError() = runTest {
        val service = NetworkStorageConfigService()
        val result = service.removeStorage("does-not-exist")
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is NetworkStorageException.GenericError)
        assertTrue(ex.message!!.contains("Storage not found"))
    }

    @Test
    fun testSetActiveStorageNonExistentReturnsGenericError() = runTest {
        val service = NetworkStorageConfigService()
        val result = service.setActiveStorage("does-not-exist")
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is NetworkStorageException.GenericError)
        assertTrue(ex.message!!.contains("Storage not found"))
    }
}
