/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Isolated unit tests for NetworkStorageException hierarchy
 * covering error categories, message formatting, fromThrowable
 * conversion, retryability, and edge cases.
 *
 *########################################################*/
package digital.vasic.yole.network

import digital.vasic.yole.network.common.NetworkStorageException
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Isolated unit tests for [NetworkStorageException] hierarchy.
 *
 * Validates error codes, message formatting, cause chains,
 * fromThrowable conversion, retryability, permanence,
 * user-friendly messages, and suggested actions.
 *
 * Total: 50 tests
 */
class NetworkStorageErrorUnitTest {

    // ==================== Connection Exceptions ====================

    @Test
    fun connectionFailedHasCorrectErrorCode() {
        val ex = NetworkStorageException.ConnectionException.Failed()
        assertEquals("CONNECTION_FAILED", ex.errorCode)
    }

    @Test
    fun connectionFailedDefaultMessageIsSet() {
        val ex = NetworkStorageException.ConnectionException.Failed()
        assertEquals("Connection failed", ex.message)
    }

    @Test
    fun connectionFailedWithCustomMessage() {
        val ex = NetworkStorageException.ConnectionException.Failed(message = "Cannot reach server")
        assertEquals("Cannot reach server", ex.message)
    }

    @Test
    fun connectionFailedWithCause() {
        val cause = RuntimeException("socket closed")
        val ex = NetworkStorageException.ConnectionException.Failed(cause = cause)
        assertEquals(cause, ex.cause)
    }

    @Test
    fun connectionTimeoutContainsTimeoutMs() {
        val ex = NetworkStorageException.ConnectionException.Timeout(timeoutMs = 5000L)
        assertTrue(ex.message!!.contains("5000"))
        assertEquals("CONNECTION_TIMEOUT", ex.errorCode)
    }

    @Test
    fun connectionTimeoutCustomMessage() {
        val ex = NetworkStorageException.ConnectionException.Timeout(
            message = "Request timed out",
            timeoutMs = 30000L
        )
        assertTrue(ex.message!!.contains("Request timed out"))
        assertTrue(ex.message!!.contains("30000"))
    }

    @Test
    fun authenticationExceptionContainsUsernameAndAuthType() {
        val ex = NetworkStorageException.ConnectionException.Authentication(
            authType = "OAuth2",
            username = "alice"
        )
        assertTrue(ex.message!!.contains("alice"))
        assertTrue(ex.message!!.contains("OAuth2"))
        assertEquals("alice", ex.username)
        assertEquals("OAuth2", ex.authType)
        assertEquals("AUTHENTICATION_FAILED", ex.errorCode)
    }

    @Test
    fun sslErrorWithCertificateError() {
        val ex = NetworkStorageException.ConnectionException.SslError(
            certificateError = "Certificate expired"
        )
        assertTrue(ex.message!!.contains("Certificate expired"))
        assertEquals("SSL_ERROR", ex.errorCode)
    }

    @Test
    fun sslErrorWithoutCertificateError() {
        val ex = NetworkStorageException.ConnectionException.SslError()
        assertNull(ex.certificateError)
        assertEquals("SSL_ERROR", ex.errorCode)
    }

    @Test
    fun serverUnreachableContainsHostAndPort() {
        val ex = NetworkStorageException.ConnectionException.ServerUnreachable(
            host = "ftp.example.com",
            port = 21
        )
        assertTrue(ex.message!!.contains("ftp.example.com"))
        assertTrue(ex.message!!.contains("21"))
        assertEquals("SERVER_UNREACHABLE", ex.errorCode)
    }

    @Test
    fun networkUnavailableHasCorrectErrorCode() {
        val ex = NetworkStorageException.ConnectionException.NetworkUnavailable()
        assertEquals("NETWORK_UNAVAILABLE", ex.errorCode)
    }

    @Test
    fun notConnectedHasCorrectErrorCode() {
        val ex = NetworkStorageException.ConnectionException.NotConnected()
        assertEquals("NOT_CONNECTED", ex.errorCode)
    }

    // ==================== File Operation Exceptions ====================

    @Test
    fun fileNotFoundContainsPath() {
        val ex = NetworkStorageException.FileOperationException.NotFound(filePath = "/remote/file.txt")
        assertTrue(ex.message!!.contains("/remote/file.txt"))
        assertEquals("/remote/file.txt", ex.filePath)
        assertEquals("read", ex.operation)
        assertEquals("FILE_NOT_FOUND", ex.errorCode)
    }

    @Test
    fun listFailedContainsPath() {
        val ex = NetworkStorageException.FileOperationException.ListFailed(path = "/remote/dir")
        assertTrue(ex.message!!.contains("/remote/dir"))
        assertEquals("list", ex.operation)
        assertEquals("LIST_FAILED", ex.errorCode)
    }

    @Test
    fun uploadFailedHasCorrectMetadata() {
        val ex = NetworkStorageException.FileOperationException.UploadFailed(path = "/remote/doc.md")
        assertTrue(ex.message!!.contains("/remote/doc.md"))
        assertEquals("upload", ex.operation)
        assertEquals("UPLOAD_FAILED", ex.errorCode)
    }

    @Test
    fun downloadFailedHasCorrectMetadata() {
        val ex = NetworkStorageException.FileOperationException.DownloadFailed(path = "/remote/img.png")
        assertEquals("download", ex.operation)
        assertEquals("DOWNLOAD_FAILED", ex.errorCode)
    }

    @Test
    fun deleteFailedHasCorrectMetadata() {
        val ex = NetworkStorageException.FileOperationException.DeleteFailed(path = "/tmp/file.txt")
        assertEquals("delete", ex.operation)
        assertEquals("DELETE_FAILED", ex.errorCode)
    }

    @Test
    fun permissionDeniedContainsPathAndPermission() {
        val ex = NetworkStorageException.FileOperationException.PermissionDenied(
            filePath = "/secure/file.txt",
            requiredPermission = "write",
            currentUser = "bob"
        )
        assertTrue(ex.message!!.contains("/secure/file.txt"))
        assertTrue(ex.message!!.contains("write"))
        assertEquals("PERMISSION_DENIED", ex.errorCode)
        assertEquals("write", ex.requiredPermission)
    }

    @Test
    fun alreadyExistsHasCorrectMessage() {
        val ex = NetworkStorageException.FileOperationException.AlreadyExists(filePath = "/notes.md")
        assertTrue(ex.message!!.contains("/notes.md"))
        assertEquals("FILE_ALREADY_EXISTS", ex.errorCode)
        assertFalse(ex.overwriteEnabled)
    }

    @Test
    fun insufficientSpaceFormatsBytes() {
        val ex = NetworkStorageException.FileOperationException.InsufficientSpace(
            filePath = "/large/file.bin",
            requiredSpace = 1024L * 1024L * 100L, // 100 MB
            availableSpace = 1024L * 1024L * 10L   // 10 MB
        )
        assertTrue(ex.message!!.contains("INSUFFICIENT_SPACE").not()) // not in message
        assertEquals("INSUFFICIENT_SPACE", ex.errorCode)
        assertEquals(1024L * 1024L * 100L, ex.requiredSpace)
    }

    @Test
    fun fileLockWithLockType() {
        val ex = NetworkStorageException.FileOperationException.Locked(
            filePath = "/doc.txt",
            lockType = "exclusive"
        )
        assertTrue(ex.message!!.contains("exclusive"))
        assertEquals("FILE_LOCKED", ex.errorCode)
    }

    @Test
    fun infoFailedHasCorrectMetadata() {
        val ex = NetworkStorageException.FileOperationException.InfoFailed(path = "/meta/file.txt")
        assertEquals("info", ex.operation)
        assertEquals("INFO_FAILED", ex.errorCode)
    }

    // ==================== Protocol Exceptions ====================

    @Test
    fun unsupportedProtocolListsSupportedOnes() {
        val ex = NetworkStorageException.ProtocolException.Unsupported(
            protocol = "SMB",
            supportedProtocols = listOf("FTP", "SFTP", "WebDAV")
        )
        assertTrue(ex.message!!.contains("SMB"))
        assertTrue(ex.message!!.contains("FTP"))
        assertEquals("SMB", ex.protocol)
        assertEquals("UNSUPPORTED_PROTOCOL", ex.errorCode)
    }

    @Test
    fun versionMismatchContainsVersionInfo() {
        val ex = NetworkStorageException.ProtocolException.VersionMismatch(
            protocol = "FTP",
            expectedVersion = "1.0",
            actualVersion = "2.0"
        )
        assertTrue(ex.message!!.contains("1.0"))
        assertTrue(ex.message!!.contains("2.0"))
        assertEquals("VERSION_MISMATCH", ex.errorCode)
    }

    @Test
    fun configurationErrorWithMissingKey() {
        val ex = NetworkStorageException.ProtocolException.ConfigurationError(
            protocol = "SFTP",
            configKey = "privateKeyPath",
            configValue = null,
            expectedType = "String"
        )
        assertTrue(ex.message!!.contains("privateKeyPath"))
        assertTrue(ex.message!!.contains("missing") || ex.message!!.contains("null") || ex.message!!.contains("is missing"))
        assertEquals("CONFIGURATION_ERROR", ex.errorCode)
    }

    // ==================== Sync Exceptions ====================

    @Test
    fun syncConflictContainsPathAndType() {
        val now = Clock.System.now()
        val ex = NetworkStorageException.SyncException.Conflict(
            remotePath = "/notes/plan.md",
            conflictType = NetworkStorageException.ConflictType.BOTH_MODIFIED,
            remoteTimestamp = now,
            localTimestamp = now
        )
        assertTrue(ex.message!!.contains("/notes/plan.md"))
        assertEquals("SYNC_CONFLICT", ex.errorCode)
        assertEquals("/notes/plan.md", ex.remotePath)
    }

    @Test
    fun syncInterruptedContainsByteProgress() {
        val ex = NetworkStorageException.SyncException.Interrupted(
            remotePath = "/backup/data.zip",
            operation = "upload",
            bytesProcessed = 1024L,
            totalBytes = 4096L
        )
        assertTrue(ex.message!!.contains("1024"))
        assertTrue(ex.message!!.contains("4096"))
        assertEquals("SYNC_INTERRUPTED", ex.errorCode)
    }

    @Test
    fun syncRetryLimitExceededContainsRetryInfo() {
        val ex = NetworkStorageException.SyncException.RetryLimitExceeded(
            remotePath = "/file.txt",
            retryCount = 5,
            maxRetries = 5
        )
        assertTrue(ex.message!!.contains("5"))
        assertEquals("RETRY_LIMIT_EXCEEDED", ex.errorCode)
    }

    // ==================== Quota Exceptions ====================

    @Test
    fun quotaExceededContainsSpaceInfo() {
        val ex = NetworkStorageException.QuotaException.Exceeded(
            usedSpace = 1024L * 1024L * 1024L * 15L, // 15 GB
            totalQuota = 1024L * 1024L * 1024L * 15L  // 15 GB
        )
        assertEquals("QUOTA_EXCEEDED", ex.errorCode)
        assertEquals("storage", ex.quotaType)
    }

    @Test
    fun bandwidthExceededHasResetTime() {
        val resetTime = Clock.System.now()
        val ex = NetworkStorageException.QuotaException.BandwidthExceeded(
            usedBandwidth = 1000L,
            totalBandwidth = 10000L,
            resetTime = resetTime
        )
        assertEquals("BANDWIDTH_QUOTA_EXCEEDED", ex.errorCode)
        assertEquals("bandwidth", ex.quotaType)
    }

    // ==================== Cache Exceptions ====================

    @Test
    fun cacheCorruptionWithChecksumMismatch() {
        val ex = NetworkStorageException.CacheException.Corruption(
            cachePath = "/cache/db",
            checksumMismatch = true
        )
        assertTrue(ex.message!!.contains("checksum mismatch"))
        assertEquals("CACHE_CORRUPTION", ex.errorCode)
    }

    @Test
    fun cacheEntryNotFoundContainsKey() {
        val ex = NetworkStorageException.CacheException.EntryNotFound(
            cachePath = "/cache",
            key = "doc-abc123"
        )
        assertTrue(ex.message!!.contains("doc-abc123"))
        assertEquals("CACHE_ENTRY_NOT_FOUND", ex.errorCode)
    }

    // ==================== GenericError ====================

    @Test
    fun genericErrorWithOperation() {
        val ex = NetworkStorageException.GenericError(
            message = "Something went wrong",
            operation = "listFiles",
            context = mapOf("path" to "/remote")
        )
        assertEquals("Something went wrong", ex.message)
        assertEquals("GENERIC_ERROR", ex.errorCode)
        assertEquals("listFiles", ex.operation)
    }

    // ==================== fromThrowable ====================

    @Test
    fun fromThrowableWithNetworkStorageExceptionReturnsItself() {
        val original = NetworkStorageException.ConnectionException.Failed(message = "direct")
        val result = NetworkStorageException.fromThrowable(original)
        assertEquals(original, result)
    }

    @Test
    fun fromThrowableWithTimeoutMessageCreatesTimeout() {
        val throwable = RuntimeException("Connection timeout exceeded")
        val result = NetworkStorageException.fromThrowable(throwable)
        assertIs<NetworkStorageException.ConnectionException.Timeout>(result)
    }

    @Test
    fun fromThrowableWithAuthenticationMessageCreatesAuth() {
        val throwable = RuntimeException("Authentication failed for user")
        val result = NetworkStorageException.fromThrowable(throwable)
        assertIs<NetworkStorageException.ConnectionException.Authentication>(result)
    }

    @Test
    fun fromThrowableWithPermissionMessageCreatesPermission() {
        val throwable = RuntimeException("Permission denied to access path")
        val result = NetworkStorageException.fromThrowable(throwable, filePath = "/tmp/file")
        assertIs<NetworkStorageException.FileOperationException.PermissionDenied>(result)
    }

    @Test
    fun fromThrowableWithQuotaMessageCreatesQuota() {
        val throwable = RuntimeException("Storage quota exceeded limit")
        val result = NetworkStorageException.fromThrowable(throwable)
        assertIs<NetworkStorageException.QuotaException.Exceeded>(result)
    }

    @Test
    fun fromThrowableWithGenericExceptionCreatesGenericError() {
        val throwable = RuntimeException("Unexpected IO error")
        val result = NetworkStorageException.fromThrowable(throwable, operation = "read")
        assertIs<NetworkStorageException.GenericError>(result)
        assertEquals("Unexpected IO error", result.message)
    }

    @Test
    fun fromThrowablePreservesOriginalMessage() {
        val throwable = IllegalStateException("Unexpected state encountered")
        val result = NetworkStorageException.fromThrowable(throwable)
        assertIs<NetworkStorageException.GenericError>(result)
        assertEquals("Unexpected state encountered", result.message)
    }

    // ==================== isRetryable ====================

    @Test
    fun connectionTimeoutIsRetryable() {
        val ex = NetworkStorageException.ConnectionException.Timeout(timeoutMs = 5000)
        assertTrue(ex.isRetryable())
    }

    @Test
    fun networkUnavailableIsRetryable() {
        val ex = NetworkStorageException.ConnectionException.NetworkUnavailable()
        assertTrue(ex.isRetryable())
    }

    @Test
    fun serverUnreachableIsRetryable() {
        val ex = NetworkStorageException.ConnectionException.ServerUnreachable(
            host = "example.com",
            port = 22
        )
        assertTrue(ex.isRetryable())
    }

    @Test
    fun authenticationIsNotRetryable() {
        val ex = NetworkStorageException.ConnectionException.Authentication(
            authType = "Basic",
            username = "user"
        )
        assertFalse(ex.isRetryable())
    }

    @Test
    fun permissionDeniedIsNotRetryable() {
        val ex = NetworkStorageException.FileOperationException.PermissionDenied(
            filePath = "/file",
            requiredPermission = "read"
        )
        assertFalse(ex.isRetryable())
    }

    // ==================== isPermanentFailure ====================

    @Test
    fun authenticationIsPermanentFailure() {
        val ex = NetworkStorageException.ConnectionException.Authentication(
            authType = "OAuth2",
            username = "user"
        )
        assertTrue(ex.isPermanentFailure())
    }

    @Test
    fun quotaExceededIsPermanentFailure() {
        val ex = NetworkStorageException.QuotaException.Exceeded(
            usedSpace = 100L,
            totalQuota = 100L
        )
        assertTrue(ex.isPermanentFailure())
    }

    @Test
    fun connectionTimeoutIsNotPermanentFailure() {
        val ex = NetworkStorageException.ConnectionException.Timeout(timeoutMs = 5000)
        assertFalse(ex.isPermanentFailure())
    }

    // ==================== toUserMessage ====================

    @Test
    fun timeoutUserMessageIsHelpful() {
        val ex = NetworkStorageException.ConnectionException.Timeout(timeoutMs = 30000)
        val msg = ex.toUserMessage()
        assertTrue(msg.isNotEmpty())
        assertTrue(msg.contains("timed out", ignoreCase = true) || msg.contains("internet", ignoreCase = true))
    }

    @Test
    fun authenticationUserMessageMentionsCredentials() {
        val ex = NetworkStorageException.ConnectionException.Authentication(
            authType = "Basic",
            username = "user"
        )
        val msg = ex.toUserMessage()
        assertTrue(msg.contains("credential", ignoreCase = true) || msg.contains("authentication", ignoreCase = true))
    }

    @Test
    fun fileNotFoundUserMessageContainsPath() {
        val ex = NetworkStorageException.FileOperationException.NotFound(filePath = "/notes/doc.txt")
        val msg = ex.toUserMessage()
        assertTrue(msg.contains("/notes/doc.txt"))
    }

    // ==================== getSuggestedAction ====================

    @Test
    fun timeoutSuggestsInternetCheck() {
        val ex = NetworkStorageException.ConnectionException.Timeout(timeoutMs = 10000)
        val action = ex.getSuggestedAction()
        assertNotNull(action)
        assertTrue(action.contains("internet", ignoreCase = true) || action.contains("connection", ignoreCase = true))
    }

    @Test
    fun fileNotFoundSuggestsPathCheck() {
        val ex = NetworkStorageException.FileOperationException.NotFound(filePath = "/path")
        val action = ex.getSuggestedAction()
        assertNotNull(action)
    }

    @Test
    fun genericErrorReturnsNullAction() {
        val ex = NetworkStorageException.GenericError(message = "Something unexpected")
        val action = ex.getSuggestedAction()
        assertNull(action)
    }

    // ==================== Timestamp ====================

    @Test
    fun exceptionsHaveTimestampSet() {
        val before = Clock.System.now()
        val ex = NetworkStorageException.ConnectionException.Failed()
        val after = Clock.System.now()
        assertTrue(ex.timestamp >= before)
        assertTrue(ex.timestamp <= after)
    }

    // ==================== ConflictType Enum ====================

    @Test
    fun conflictTypeEnumHasAllExpectedValues() {
        val values = NetworkStorageException.ConflictType.entries
        assertTrue(values.any { it == NetworkStorageException.ConflictType.BOTH_MODIFIED })
        assertTrue(values.any { it == NetworkStorageException.ConflictType.REMOTE_MODIFIED })
        assertTrue(values.any { it == NetworkStorageException.ConflictType.LOCAL_MODIFIED })
        assertTrue(values.any { it == NetworkStorageException.ConflictType.DELETE_MODIFY_CONFLICT })
        assertTrue(values.any { it == NetworkStorageException.ConflictType.RENAME_CONFLICT })
        assertTrue(values.any { it == NetworkStorageException.ConflictType.UNKNOWN })
    }

    // ==================== Property-Based / Fuzz ====================

    @Test
    fun fromThrowableHandlesNullMessage() {
        val throwable = object : Throwable() {
            override val message: String? = null
        }
        val result = NetworkStorageException.fromThrowable(throwable)
        assertIs<NetworkStorageException.GenericError>(result)
        // Should not crash even with null message
        assertNotNull(result)
    }

    @Test
    fun randomExceptionMessagesDoNotCrashFromThrowable() {
        val messages = listOf(
            "", "   ", "timeout", "AUTHENTICATION", "Permission", "quota", "some random text",
            "1234567890", "!@#\$%^&*()", "null", "undefined"
        )
        for (msg in messages) {
            val throwable = RuntimeException(msg)
            val result = NetworkStorageException.fromThrowable(throwable)
            assertNotNull(result)
            assertTrue(result.errorCode.isNotEmpty())
        }
    }
}
