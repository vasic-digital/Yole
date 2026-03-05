/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for NetworkStorageException hierarchy
 *
 *########################################################*/
package digital.vasic.yole.network.common

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NetworkStorageExceptionTest {

    // ====== Connection Exceptions ======

    @Test
    fun testConnectionFailed() {
        val ex = NetworkStorageException.ConnectionException.Failed(
            message = "Server unreachable",
            cause = RuntimeException("timeout")
        )
        assertEquals("Server unreachable", ex.message)
        assertEquals("CONNECTION_FAILED", ex.errorCode)
        assertNotNull(ex.cause)
        assertNotNull(ex.timestamp)
    }

    @Test
    fun testConnectionFailedDefaultMessage() {
        val ex = NetworkStorageException.ConnectionException.Failed()
        assertEquals("Connection failed", ex.message)
        assertNull(ex.cause)
    }

    @Test
    fun testConnectionTimeout() {
        val ex = NetworkStorageException.ConnectionException.Timeout(timeoutMs = 30000)
        assertTrue(ex.message!!.contains("30000"))
        assertEquals("CONNECTION_TIMEOUT", ex.errorCode)
    }

    @Test
    fun testAuthenticationFailed() {
        val ex = NetworkStorageException.ConnectionException.Authentication(
            authType = "OAuth2",
            username = "testuser"
        )
        assertTrue(ex.message!!.contains("testuser"))
        assertTrue(ex.message!!.contains("OAuth2"))
        assertEquals("OAuth2", ex.authType)
        assertEquals("testuser", ex.username)
    }

    @Test
    fun testSslError() {
        val ex = NetworkStorageException.ConnectionException.SslError(
            certificateError = "expired certificate"
        )
        assertTrue(ex.message!!.contains("expired certificate"))
        assertEquals("SSL_ERROR", ex.errorCode)
    }

    @Test
    fun testSslErrorNoCertificateError() {
        val ex = NetworkStorageException.ConnectionException.SslError()
        assertEquals("SSL/TLS error", ex.message)
    }

    @Test
    fun testServerUnreachable() {
        val ex = NetworkStorageException.ConnectionException.ServerUnreachable(
            host = "example.com",
            port = 443
        )
        assertTrue(ex.message!!.contains("example.com"))
        assertTrue(ex.message!!.contains("443"))
        assertEquals("SERVER_UNREACHABLE", ex.errorCode)
        assertEquals("example.com", ex.host)
        assertEquals(443, ex.port)
    }

    @Test
    fun testNetworkUnavailable() {
        val ex = NetworkStorageException.ConnectionException.NetworkUnavailable()
        assertEquals("Network unavailable", ex.message)
        assertEquals("NETWORK_UNAVAILABLE", ex.errorCode)
    }

    @Test
    fun testNotConnected() {
        val ex = NetworkStorageException.ConnectionException.NotConnected()
        assertEquals("Not connected", ex.message)
        assertEquals("NOT_CONNECTED", ex.errorCode)
    }

    // ====== File Operation Exceptions ======

    @Test
    fun testFileNotFound() {
        val ex = NetworkStorageException.FileOperationException.NotFound(
            filePath = "/remote/missing.txt",
            searchPaths = listOf("/path1", "/path2")
        )
        assertTrue(ex.message!!.contains("/remote/missing.txt"))
        assertEquals("FILE_NOT_FOUND", ex.errorCode)
        assertEquals("/remote/missing.txt", ex.filePath)
        assertEquals("read", ex.operation)
        assertEquals(2, ex.searchPaths.size)
    }

    @Test
    fun testListFailed() {
        val ex = NetworkStorageException.FileOperationException.ListFailed(path = "/folder")
        assertTrue(ex.message!!.contains("/folder"))
        assertEquals("LIST_FAILED", ex.errorCode)
        assertEquals("list", ex.operation)
    }

    @Test
    fun testUploadFailed() {
        val ex = NetworkStorageException.FileOperationException.UploadFailed(path = "/upload.txt")
        assertTrue(ex.message!!.contains("/upload.txt"))
        assertEquals("UPLOAD_FAILED", ex.errorCode)
        assertEquals("upload", ex.operation)
    }

    @Test
    fun testDownloadFailed() {
        val ex = NetworkStorageException.FileOperationException.DownloadFailed(path = "/download.txt")
        assertTrue(ex.message!!.contains("/download.txt"))
        assertEquals("DOWNLOAD_FAILED", ex.errorCode)
        assertEquals("download", ex.operation)
    }

    @Test
    fun testCreateFolderFailed() {
        val ex = NetworkStorageException.FileOperationException.CreateFolderFailed(path = "/newfolder")
        assertTrue(ex.message!!.contains("/newfolder"))
        assertEquals("CREATE_FOLDER_FAILED", ex.errorCode)
        assertEquals("create_folder", ex.operation)
    }

    @Test
    fun testCopyFailed() {
        val ex = NetworkStorageException.FileOperationException.CopyFailed(
            sourcePath = "/source.txt",
            targetPath = "/target.txt"
        )
        assertTrue(ex.message!!.contains("/source.txt"))
        assertTrue(ex.message!!.contains("/target.txt"))
        assertEquals("COPY_FAILED", ex.errorCode)
        assertEquals("copy", ex.operation)
    }

    @Test
    fun testMoveFailed() {
        val ex = NetworkStorageException.FileOperationException.MoveFailed(
            sourcePath = "/old.txt",
            targetPath = "/new.txt"
        )
        assertTrue(ex.message!!.contains("/old.txt"))
        assertTrue(ex.message!!.contains("/new.txt"))
        assertEquals("MOVE_FAILED", ex.errorCode)
        assertEquals("move", ex.operation)
    }

    @Test
    fun testDeleteFailed() {
        val ex = NetworkStorageException.FileOperationException.DeleteFailed(path = "/delete.txt")
        assertTrue(ex.message!!.contains("/delete.txt"))
        assertEquals("DELETE_FAILED", ex.errorCode)
        assertEquals("delete", ex.operation)
    }

    @Test
    fun testPermissionDenied() {
        val ex = NetworkStorageException.FileOperationException.PermissionDenied(
            filePath = "/secret.txt",
            requiredPermission = "write",
            currentUser = "john"
        )
        assertTrue(ex.message!!.contains("/secret.txt"))
        assertTrue(ex.message!!.contains("write"))
        assertTrue(ex.message!!.contains("john"))
        assertEquals("PERMISSION_DENIED", ex.errorCode)
        assertEquals("access", ex.operation)
    }

    @Test
    fun testPermissionDeniedNoUser() {
        val ex = NetworkStorageException.FileOperationException.PermissionDenied(
            filePath = "/secret.txt",
            requiredPermission = "read"
        )
        assertFalse(ex.message!!.contains("for user"))
    }

    @Test
    fun testAlreadyExists() {
        val ex = NetworkStorageException.FileOperationException.AlreadyExists(
            filePath = "/existing.txt",
            overwriteEnabled = true
        )
        assertTrue(ex.message!!.contains("/existing.txt"))
        assertEquals("FILE_ALREADY_EXISTS", ex.errorCode)
        assertEquals("create", ex.operation)
        assertTrue(ex.overwriteEnabled)
    }

    @Test
    fun testInsufficientSpace() {
        val ex = NetworkStorageException.FileOperationException.InsufficientSpace(
            filePath = "/largefile.zip",
            requiredSpace = 1073741824L,
            availableSpace = 1048576L
        )
        assertTrue(ex.message!!.contains("/largefile.zip"))
        assertEquals("INSUFFICIENT_SPACE", ex.errorCode)
        assertEquals("write", ex.operation)
        assertEquals(1073741824L, ex.requiredSpace)
        assertEquals(1048576L, ex.availableSpace)
    }

    @Test
    fun testLocked() {
        val ex = NetworkStorageException.FileOperationException.Locked(
            filePath = "/locked.txt",
            lockType = "exclusive"
        )
        assertTrue(ex.message!!.contains("/locked.txt"))
        assertTrue(ex.message!!.contains("exclusive"))
        assertEquals("FILE_LOCKED", ex.errorCode)
        assertEquals("modify", ex.operation)
    }

    @Test
    fun testLockedNoLockType() {
        val ex = NetworkStorageException.FileOperationException.Locked(filePath = "/locked.txt")
        assertNull(ex.lockType)
    }

    @Test
    fun testInfoFailed() {
        val ex = NetworkStorageException.FileOperationException.InfoFailed(path = "/info.txt")
        assertTrue(ex.message!!.contains("/info.txt"))
        assertEquals("INFO_FAILED", ex.errorCode)
        assertEquals("info", ex.operation)
    }

    // ====== Protocol Exceptions ======

    @Test
    fun testProtocolUnsupported() {
        val ex = NetworkStorageException.ProtocolException.Unsupported(
            protocol = "nfs",
            supportedProtocols = listOf("webdav", "ftp", "sftp")
        )
        assertTrue(ex.message!!.contains("nfs"))
        assertTrue(ex.message!!.contains("webdav"))
        assertEquals("UNSUPPORTED_PROTOCOL", ex.errorCode)
        assertEquals("nfs", ex.protocol)
    }

    @Test
    fun testProtocolVersionMismatch() {
        val ex = NetworkStorageException.ProtocolException.VersionMismatch(
            protocol = "smb",
            expectedVersion = "3.0",
            actualVersion = "2.0"
        )
        assertTrue(ex.message!!.contains("smb"))
        assertTrue(ex.message!!.contains("3.0"))
        assertTrue(ex.message!!.contains("2.0"))
        assertEquals("VERSION_MISMATCH", ex.errorCode)
    }

    @Test
    fun testProtocolConfigurationError() {
        val ex = NetworkStorageException.ProtocolException.ConfigurationError(
            protocol = "ftp",
            configKey = "port",
            configValue = "abc",
            expectedType = "Int"
        )
        assertTrue(ex.message!!.contains("ftp"))
        assertTrue(ex.message!!.contains("port"))
        assertTrue(ex.message!!.contains("abc"))
        assertEquals("CONFIGURATION_ERROR", ex.errorCode)
    }

    @Test
    fun testProtocolConfigurationErrorNullValue() {
        val ex = NetworkStorageException.ProtocolException.ConfigurationError(
            protocol = "ftp",
            configKey = "host",
            configValue = null,
            expectedType = "String"
        )
        assertTrue(ex.message!!.contains("is missing"))
    }

    // ====== Sync Exceptions ======

    @Test
    fun testSyncConflict() {
        val now = Clock.System.now()
        val ex = NetworkStorageException.SyncException.Conflict(
            remotePath = "/doc.txt",
            localPath = "/local/doc.txt",
            conflictType = NetworkStorageException.ConflictType.BOTH_MODIFIED,
            remoteTimestamp = now,
            localTimestamp = now
        )
        assertTrue(ex.message!!.contains("/doc.txt"))
        assertTrue(ex.message!!.contains("BOTH_MODIFIED"))
        assertEquals("SYNC_CONFLICT", ex.errorCode)
        assertEquals("/doc.txt", ex.remotePath)
        assertEquals("/local/doc.txt", ex.localPath)
    }

    @Test
    fun testSyncInterrupted() {
        val ex = NetworkStorageException.SyncException.Interrupted(
            remotePath = "/large.zip",
            operation = "upload",
            bytesProcessed = 500L,
            totalBytes = 1000L
        )
        assertTrue(ex.message!!.contains("upload"))
        assertTrue(ex.message!!.contains("500"))
        assertTrue(ex.message!!.contains("1000"))
        assertEquals("SYNC_INTERRUPTED", ex.errorCode)
    }

    @Test
    fun testSyncRetryLimitExceeded() {
        val ex = NetworkStorageException.SyncException.RetryLimitExceeded(
            remotePath = "/retry.txt",
            retryCount = 5,
            maxRetries = 5
        )
        assertTrue(ex.message!!.contains("5/5"))
        assertEquals("RETRY_LIMIT_EXCEEDED", ex.errorCode)
    }

    // ====== Quota Exceptions ======

    @Test
    fun testQuotaExceeded() {
        val ex = NetworkStorageException.QuotaException.Exceeded(
            usedSpace = 1073741824L,
            totalQuota = 1073741824L
        )
        assertEquals("QUOTA_EXCEEDED", ex.errorCode)
        assertEquals("storage", ex.quotaType)
    }

    @Test
    fun testBandwidthExceeded() {
        val now = Clock.System.now()
        val ex = NetworkStorageException.QuotaException.BandwidthExceeded(
            usedBandwidth = 5368709120L,
            totalBandwidth = 5368709120L,
            resetTime = now
        )
        assertEquals("BANDWIDTH_QUOTA_EXCEEDED", ex.errorCode)
        assertEquals("bandwidth", ex.quotaType)
    }

    // ====== Cache Exceptions ======

    @Test
    fun testCacheCorruption() {
        val ex = NetworkStorageException.CacheException.Corruption(
            cachePath = "/cache/data",
            checksumMismatch = true,
            expectedSize = 1024L,
            actualSize = 512L
        )
        assertTrue(ex.message!!.contains("checksum mismatch"))
        assertEquals("CACHE_CORRUPTION", ex.errorCode)
        assertEquals("/cache/data", ex.cachePath)
    }

    @Test
    fun testCacheCorruptionMinimal() {
        val ex = NetworkStorageException.CacheException.Corruption(cachePath = "/cache")
        assertFalse(ex.message!!.contains("checksum mismatch"))
    }

    @Test
    fun testCacheEntryNotFound() {
        val ex = NetworkStorageException.CacheException.EntryNotFound(
            cachePath = "/cache",
            key = "doc123"
        )
        assertTrue(ex.message!!.contains("doc123"))
        assertEquals("CACHE_ENTRY_NOT_FOUND", ex.errorCode)
    }

    // ====== Generic Error ======

    @Test
    fun testGenericError() {
        val ex = NetworkStorageException.GenericError(
            message = "Something went wrong",
            operation = "sync",
            context = mapOf("retry" to true)
        )
        assertEquals("Something went wrong", ex.message)
        assertEquals("GENERIC_ERROR", ex.errorCode)
        assertEquals("sync", ex.operation)
    }

    // ====== ConflictType Enum ======

    @Test
    fun testConflictTypeValues() {
        assertEquals(6, NetworkStorageException.ConflictType.entries.size)
        NetworkStorageException.ConflictType.BOTH_MODIFIED
        NetworkStorageException.ConflictType.REMOTE_MODIFIED
        NetworkStorageException.ConflictType.LOCAL_MODIFIED
        NetworkStorageException.ConflictType.DELETE_MODIFY_CONFLICT
        NetworkStorageException.ConflictType.RENAME_CONFLICT
        NetworkStorageException.ConflictType.UNKNOWN
    }

    // ====== Utility Methods ======

    @Test
    fun testToUserMessageTimeout() {
        val ex = NetworkStorageException.ConnectionException.Timeout(timeoutMs = 5000)
        assertTrue(ex.toUserMessage().contains("timed out"))
    }

    @Test
    fun testToUserMessageAuthentication() {
        val ex = NetworkStorageException.ConnectionException.Authentication(
            authType = "basic", username = "user"
        )
        assertTrue(ex.toUserMessage().contains("Authentication failed"))
    }

    @Test
    fun testToUserMessageSslError() {
        val ex = NetworkStorageException.ConnectionException.SslError()
        assertTrue(ex.toUserMessage().contains("Secure connection"))
    }

    @Test
    fun testToUserMessageServerUnreachable() {
        val ex = NetworkStorageException.ConnectionException.ServerUnreachable(
            host = "test.com", port = 80
        )
        assertTrue(ex.toUserMessage().contains("unreachable"))
    }

    @Test
    fun testToUserMessageNetworkUnavailable() {
        val ex = NetworkStorageException.ConnectionException.NetworkUnavailable()
        assertTrue(ex.toUserMessage().contains("unavailable"))
    }

    @Test
    fun testToUserMessageNotFound() {
        val ex = NetworkStorageException.FileOperationException.NotFound(filePath = "/missing.txt")
        assertTrue(ex.toUserMessage().contains("not found"))
    }

    @Test
    fun testToUserMessagePermissionDenied() {
        val ex = NetworkStorageException.FileOperationException.PermissionDenied(
            filePath = "/secret.txt", requiredPermission = "write"
        )
        assertTrue(ex.toUserMessage().contains("Permission denied"))
    }

    @Test
    fun testToUserMessageAlreadyExists() {
        val ex = NetworkStorageException.FileOperationException.AlreadyExists(filePath = "/dup.txt")
        assertTrue(ex.toUserMessage().contains("already exists"))
    }

    @Test
    fun testToUserMessageInsufficientSpace() {
        val ex = NetworkStorageException.FileOperationException.InsufficientSpace(
            filePath = "/big.zip", requiredSpace = 1000L, availableSpace = 100L
        )
        assertTrue(ex.toUserMessage().contains("Not enough space"))
    }

    @Test
    fun testToUserMessageLocked() {
        val ex = NetworkStorageException.FileOperationException.Locked(filePath = "/locked.txt")
        assertTrue(ex.toUserMessage().contains("locked"))
    }

    @Test
    fun testToUserMessageUnsupported() {
        val ex = NetworkStorageException.ProtocolException.Unsupported(
            protocol = "nfs", supportedProtocols = listOf("ftp")
        )
        assertTrue(ex.toUserMessage().contains("Unsupported"))
    }

    @Test
    fun testToUserMessageConflict() {
        val now = Clock.System.now()
        val ex = NetworkStorageException.SyncException.Conflict(
            remotePath = "/doc.txt",
            conflictType = NetworkStorageException.ConflictType.BOTH_MODIFIED,
            remoteTimestamp = now, localTimestamp = now
        )
        assertTrue(ex.toUserMessage().contains("conflict"))
    }

    @Test
    fun testToUserMessageQuotaExceeded() {
        val ex = NetworkStorageException.QuotaException.Exceeded(
            usedSpace = 100L, totalQuota = 100L
        )
        assertTrue(ex.toUserMessage().contains("quota exceeded"))
    }

    @Test
    fun testToUserMessageBandwidthExceeded() {
        val ex = NetworkStorageException.QuotaException.BandwidthExceeded(
            usedBandwidth = 100L, totalBandwidth = 100L, resetTime = Clock.System.now()
        )
        assertTrue(ex.toUserMessage().contains("Bandwidth"))
    }

    @Test
    fun testToUserMessageCacheCorruption() {
        val ex = NetworkStorageException.CacheException.Corruption(cachePath = "/cache")
        assertTrue(ex.toUserMessage().contains("Cache corruption"))
    }

    @Test
    fun testToUserMessageCacheEntryNotFound() {
        val ex = NetworkStorageException.CacheException.EntryNotFound(
            cachePath = "/cache", key = "key1"
        )
        assertTrue(ex.toUserMessage().contains("not found"))
    }

    @Test
    fun testToUserMessageGeneric() {
        val ex = NetworkStorageException.GenericError(message = "custom error")
        assertTrue(ex.toUserMessage().contains("custom error"))
    }

    // ====== isRetryable ======

    @Test
    fun testIsRetryableTimeout() {
        val ex = NetworkStorageException.ConnectionException.Timeout(timeoutMs = 5000)
        assertTrue(ex.isRetryable())
    }

    @Test
    fun testIsRetryableNetworkUnavailable() {
        val ex = NetworkStorageException.ConnectionException.NetworkUnavailable()
        assertTrue(ex.isRetryable())
    }

    @Test
    fun testIsRetryableServerUnreachable() {
        val ex = NetworkStorageException.ConnectionException.ServerUnreachable(
            host = "test.com", port = 80
        )
        assertTrue(ex.isRetryable())
    }

    @Test
    fun testIsRetryableBandwidthExceeded() {
        val ex = NetworkStorageException.QuotaException.BandwidthExceeded(
            usedBandwidth = 100L, totalBandwidth = 100L, resetTime = Clock.System.now()
        )
        assertTrue(ex.isRetryable())
    }

    @Test
    fun testIsNotRetryableAuthentication() {
        val ex = NetworkStorageException.ConnectionException.Authentication(
            authType = "basic", username = "user"
        )
        assertFalse(ex.isRetryable())
    }

    @Test
    fun testIsNotRetryableNotFound() {
        val ex = NetworkStorageException.FileOperationException.NotFound(filePath = "/gone.txt")
        assertFalse(ex.isRetryable())
    }

    // ====== isPermanentFailure ======

    @Test
    fun testIsPermanentFailureAuthentication() {
        val ex = NetworkStorageException.ConnectionException.Authentication(
            authType = "basic", username = "user"
        )
        assertTrue(ex.isPermanentFailure())
    }

    @Test
    fun testIsPermanentFailurePermissionDenied() {
        val ex = NetworkStorageException.FileOperationException.PermissionDenied(
            filePath = "/secret.txt", requiredPermission = "write"
        )
        assertTrue(ex.isPermanentFailure())
    }

    @Test
    fun testIsPermanentFailureUnsupported() {
        val ex = NetworkStorageException.ProtocolException.Unsupported(
            protocol = "nfs", supportedProtocols = listOf("ftp")
        )
        assertTrue(ex.isPermanentFailure())
    }

    @Test
    fun testIsPermanentFailureVersionMismatch() {
        val ex = NetworkStorageException.ProtocolException.VersionMismatch(
            protocol = "smb", expectedVersion = "3", actualVersion = "1"
        )
        assertTrue(ex.isPermanentFailure())
    }

    @Test
    fun testIsPermanentFailureQuotaExceeded() {
        val ex = NetworkStorageException.QuotaException.Exceeded(
            usedSpace = 100L, totalQuota = 100L
        )
        assertTrue(ex.isPermanentFailure())
    }

    @Test
    fun testIsNotPermanentFailureTimeout() {
        val ex = NetworkStorageException.ConnectionException.Timeout(timeoutMs = 5000)
        assertFalse(ex.isPermanentFailure())
    }

    // ====== getSuggestedAction ======

    @Test
    fun testSuggestedActionTimeout() {
        val ex = NetworkStorageException.ConnectionException.Timeout(timeoutMs = 5000)
        assertNotNull(ex.getSuggestedAction())
        assertTrue(ex.getSuggestedAction()!!.contains("internet"))
    }

    @Test
    fun testSuggestedActionAuthentication() {
        val ex = NetworkStorageException.ConnectionException.Authentication(
            authType = "basic", username = "user"
        )
        assertNotNull(ex.getSuggestedAction())
        assertTrue(ex.getSuggestedAction()!!.contains("credentials"))
    }

    @Test
    fun testSuggestedActionSsl() {
        val ex = NetworkStorageException.ConnectionException.SslError()
        assertNotNull(ex.getSuggestedAction())
        assertTrue(ex.getSuggestedAction()!!.contains("SSL"))
    }

    @Test
    fun testSuggestedActionNotFound() {
        val ex = NetworkStorageException.FileOperationException.NotFound(filePath = "/x")
        assertNotNull(ex.getSuggestedAction())
    }

    @Test
    fun testSuggestedActionPermissionDenied() {
        val ex = NetworkStorageException.FileOperationException.PermissionDenied(
            filePath = "/x", requiredPermission = "write"
        )
        assertNotNull(ex.getSuggestedAction())
    }

    @Test
    fun testSuggestedActionInsufficientSpace() {
        val ex = NetworkStorageException.FileOperationException.InsufficientSpace(
            filePath = "/x", requiredSpace = 100L, availableSpace = 10L
        )
        assertNotNull(ex.getSuggestedAction())
    }

    @Test
    fun testSuggestedActionConflict() {
        val now = Clock.System.now()
        val ex = NetworkStorageException.SyncException.Conflict(
            remotePath = "/x",
            conflictType = NetworkStorageException.ConflictType.BOTH_MODIFIED,
            remoteTimestamp = now, localTimestamp = now
        )
        assertNotNull(ex.getSuggestedAction())
    }

    @Test
    fun testSuggestedActionQuotaExceeded() {
        val ex = NetworkStorageException.QuotaException.Exceeded(
            usedSpace = 100L, totalQuota = 100L
        )
        assertNotNull(ex.getSuggestedAction())
    }

    @Test
    fun testSuggestedActionCacheCorruption() {
        val ex = NetworkStorageException.CacheException.Corruption(cachePath = "/cache")
        assertNotNull(ex.getSuggestedAction())
    }

    @Test
    fun testSuggestedActionGenericReturnsNull() {
        val ex = NetworkStorageException.GenericError(message = "unknown")
        assertNull(ex.getSuggestedAction())
    }

    // ====== fromThrowable ======

    @Test
    fun testFromThrowableCreatesException() {
        val cause = RuntimeException("test error")
        val ex = NetworkStorageException.fromThrowable(cause, operation = "download")
        assertNotNull(ex)
        assertTrue(ex is NetworkStorageException.GenericError)
        assertEquals("test error", ex.message)
        assertEquals(cause, ex.cause)
        assertEquals("download", (ex as NetworkStorageException.GenericError).operation)
    }

    @Test
    fun testFromThrowableReturnsExistingException() {
        val original = NetworkStorageException.ConnectionException.Failed()
        val result = NetworkStorageException.fromThrowable(original)
        assertTrue(result === original)
    }

    @Test
    fun testFromThrowableDetectsTimeout() {
        val cause = RuntimeException("Connection timeout occurred")
        val ex = NetworkStorageException.fromThrowable(cause)
        assertTrue(ex is NetworkStorageException.ConnectionException.Timeout)
    }

    @Test
    fun testFromThrowableDetectsAuthentication() {
        val cause = RuntimeException("Authentication required")
        val ex = NetworkStorageException.fromThrowable(cause)
        assertTrue(ex is NetworkStorageException.ConnectionException.Authentication)
    }

    @Test
    fun testFromThrowableDetectsPermission() {
        val cause = RuntimeException("Permission denied for operation")
        val ex = NetworkStorageException.fromThrowable(cause, filePath = "/test.txt")
        assertTrue(ex is NetworkStorageException.FileOperationException.PermissionDenied)
    }

    @Test
    fun testFromThrowableDetectsQuota() {
        val cause = RuntimeException("Storage quota exceeded")
        val ex = NetworkStorageException.fromThrowable(cause)
        assertTrue(ex is NetworkStorageException.QuotaException.Exceeded)
    }

    @Test
    fun testFromThrowableGenericFallback() {
        val cause = RuntimeException("some random error")
        val ex = NetworkStorageException.fromThrowable(cause, operation = "upload", remotePath = "/remote")
        assertTrue(ex is NetworkStorageException.GenericError)
        val generic = ex as NetworkStorageException.GenericError
        assertEquals("upload", generic.operation)
    }
}
