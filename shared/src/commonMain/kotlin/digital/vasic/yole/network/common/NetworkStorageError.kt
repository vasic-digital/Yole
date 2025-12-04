package digital.vasic.yole.network.common

import kotlinx.datetime.Instant

/**
 * Base class for network storage related exceptions.
 * Provides structured error information for better error handling and user feedback.
 */
sealed class NetworkStorageException(
    message: String,
    cause: Throwable? = null,
    val errorCode: String,
    val timestamp: Instant = kotlinx.datetime.Clock.System.now()
) : Exception(message, cause) {
    
    /**
     * Connection-related exceptions
     */
    sealed class ConnectionException(
        message: String,
        cause: Throwable? = null,
        errorCode: String,
        details: Map<String, String> = emptyMap()
    ) : NetworkStorageException(message, cause, errorCode) {
        
        /**
         * Connection timeout occurred
         */
        class Timeout(
            message: String = "Connection timeout",
            timeoutMs: Long,
            cause: Throwable? = null
        ) : ConnectionException(
            message = "$message (${timeoutMs}ms)",
            cause = cause,
            errorCode = "CONNECTION_TIMEOUT",
            details = mapOf("timeoutMs" to timeoutMs.toString())
        )
        
        /**
         * Authentication failed
         */
        class Authentication(
            message: String = "Authentication failed",
            cause: Throwable? = null,
            val authType: String,
            val username: String
        ) : ConnectionException(
            message = "$message for user '$username' using $authType",
            cause = cause,
            errorCode = "AUTHENTICATION_FAILED",
            details = mapOf(
                "authType" to authType,
                "username" to username
            )
        )
        
        /**
         * SSL/TLS error occurred
         */
        class SslError(
            message: String = "SSL/TLS error",
            cause: Throwable? = null,
            val certificateError: String? = null
        ) : ConnectionException(
            message = if (certificateError != null) "$message: $certificateError" else message,
            cause = cause,
            errorCode = "SSL_ERROR",
            details = certificateError?.let { mapOf("certificateError" to it) } ?: emptyMap()
        )
        
        /**
         * Server unreachable
         */
        class ServerUnreachable(
            message: String = "Server unreachable",
            cause: Throwable? = null,
            val host: String,
            val port: Int
        ) : ConnectionException(
            message = "$message: $host:$port",
            cause = cause,
            errorCode = "SERVER_UNREACHABLE",
            details = mapOf("host" to host, "port" to port.toString())
        )
        
        /**
         * Network unavailable
         */
        class NetworkUnavailable(
            message: String = "Network unavailable",
            cause: Throwable? = null
        ) : ConnectionException(
            message = message,
            cause = cause,
            errorCode = "NETWORK_UNAVAILABLE"
        )
    }
    
    /**
     * File operation related exceptions
     */
    sealed class FileOperationException(
        message: String,
        cause: Throwable? = null,
        errorCode: String,
        val filePath: String,
        val operation: String
    ) : NetworkStorageException(message, cause, errorCode) {
        
        /**
         * File not found
         */
        class NotFound(
            message: String = "File not found",
            cause: Throwable? = null,
            filePath: String,
            val searchPaths: List<String> = emptyList()
        ) : FileOperationException(
            message = "$message: $filePath",
            cause = cause,
            errorCode = "FILE_NOT_FOUND",
            filePath = filePath,
            operation = "read"
        )
        
        /**
         * Permission denied
         */
        class PermissionDenied(
            message: String = "Permission denied",
            cause: Throwable? = null,
            filePath: String,
            val requiredPermission: String,
            val currentUser: String? = null
        ) : FileOperationException(
            message = "$message: $filePath (requires $requiredPermission${currentUser?.let { " for user $it" } ?: ""})",
            cause = cause,
            errorCode = "PERMISSION_DENIED",
            filePath = filePath,
            operation = "access"
        )
        
        /**
         * File already exists
         */
        class AlreadyExists(
            message: String = "File already exists",
            cause: Throwable? = null,
            filePath: String,
            val overwriteEnabled: Boolean = false
        ) : FileOperationException(
            message = "$message: $filePath${if (overwriteEnabled) " (overwrite disabled)" else ""}",
            cause = cause,
            errorCode = "FILE_ALREADY_EXISTS",
            filePath = filePath,
            operation = "create"
        )
        
        /**
         * Disk space insufficient
         */
        class InsufficientSpace(
            message: String = "Insufficient disk space",
            cause: Throwable? = null,
            filePath: String,
            val requiredSpace: Long,
            val availableSpace: Long
        ) : FileOperationException(
            message = "$message: $filePath (required: ${formatBytes(requiredSpace)}, available: ${formatBytes(availableSpace)})",
            cause = cause,
            errorCode = "INSUFFICIENT_SPACE",
            filePath = filePath,
            operation = "write"
        )
        
        /**
         * File is locked
         */
        class Locked(
            message: String = "File is locked",
            cause: Throwable? = null,
            filePath: String,
            val lockType: String? = null
        ) : FileOperationException(
            message = "$message: $filePath${lockType?.let { " ($lockType lock)" } ?: ""}",
            cause = cause,
            errorCode = "FILE_LOCKED",
            filePath = filePath,
            operation = "modify"
        )
    }
    
    /**
     * Protocol-specific exceptions
     */
    sealed class ProtocolException(
        message: String,
        cause: Throwable? = null,
        errorCode: String,
        val protocol: String
    ) : NetworkStorageException(message, cause, errorCode) {
        
        /**
         * Protocol not supported
         */
        class Unsupported(
            message: String = "Protocol not supported",
            cause: Throwable? = null,
            protocol: String,
            val supportedProtocols: List<String>
        ) : ProtocolException(
            message = "$message: $protocol (supported: ${supportedProtocols.joinToString(", ")})",
            cause = cause,
            errorCode = "UNSUPPORTED_PROTOCOL",
            protocol = protocol
        )
        
        /**
         * Protocol version mismatch
         */
        class VersionMismatch(
            message: String = "Protocol version mismatch",
            cause: Throwable? = null,
            protocol: String,
            val expectedVersion: String,
            val actualVersion: String
        ) : ProtocolException(
            message = "$message: $protocol (expected: $expectedVersion, actual: $actualVersion)",
            cause = cause,
            errorCode = "VERSION_MISMATCH",
            protocol = protocol
        )
        
        /**
         * Protocol configuration error
         */
        class ConfigurationError(
            message: String = "Protocol configuration error",
            cause: Throwable? = null,
            protocol: String,
            val configKey: String,
            val configValue: String?,
            val expectedType: String
        ) : ProtocolException(
            message = "$message: $protocol ($configKey${configValue?.let { " = $it" } ?: " is missing"} (expected type: $expectedType))",
            cause = cause,
            errorCode = "CONFIGURATION_ERROR",
            protocol = protocol
        )
    }
    
    /**
     * Synchronization exceptions
     */
    sealed class SyncException(
        message: String,
        cause: Throwable? = null,
        errorCode: String,
        val remotePath: String,
        val localPath: String? = null
    ) : NetworkStorageException(message, cause, errorCode) {
        
        /**
         * Sync conflict detected
         */
        class Conflict(
            message: String = "Sync conflict detected",
            cause: Throwable? = null,
            remotePath: String,
            localPath: String? = null,
            val conflictType: ConflictType,
            val remoteTimestamp: Instant,
            val localTimestamp: Instant
        ) : SyncException(
            message = "$message: $remotePath (${conflictType.name} conflict between $remoteTimestamp and $localTimestamp)",
            cause = cause,
            errorCode = "SYNC_CONFLICT",
            remotePath = remotePath,
            localPath = localPath
        )
        
        /**
         * Sync was interrupted
         */
        class Interrupted(
            message: String = "Sync interrupted",
            cause: Throwable? = null,
            remotePath: String,
            localPath: String? = null,
            val operation: String,
            val bytesProcessed: Long,
            val totalBytes: Long
        ) : SyncException(
            message = "$message: $operation ($bytesProcessed/$totalBytes bytes)",
            cause = cause,
            errorCode = "SYNC_INTERRUPTED",
            remotePath = remotePath,
            localPath = localPath
        )
        
        /**
         * Sync retry limit exceeded
         */
        class RetryLimitExceeded(
            message: String = "Sync retry limit exceeded",
            cause: Throwable? = null,
            remotePath: String,
            localPath: String? = null,
            val retryCount: Int,
            val maxRetries: Int
        ) : SyncException(
            message = "$message: $remotePath ($retryCount/$maxRetries retries)",
            cause = cause,
            errorCode = "RETRY_LIMIT_EXCEEDED",
            remotePath = remotePath,
            localPath = localPath
        )
    }
    
    /**
     * Quota exceptions
     */
    sealed class QuotaException(
        message: String,
        cause: Throwable? = null,
        errorCode: String,
        val quotaType: String
    ) : NetworkStorageException(message, cause, errorCode) {
        
        /**
         * Storage quota exceeded
         */
        class Exceeded(
            message: String = "Storage quota exceeded",
            cause: Throwable? = null,
            quotaType: String = "storage",
            val usedSpace: Long,
            val totalQuota: Long
        ) : QuotaException(
            message = "$message: ${formatBytes(usedSpace)}/${formatBytes(totalQuota)}",
            cause = cause,
            errorCode = "QUOTA_EXCEEDED",
            quotaType = quotaType
        )
        
        /**
         * Bandwidth quota exceeded
         */
        class BandwidthExceeded(
            message: String = "Bandwidth quota exceeded",
            cause: Throwable? = null,
            quotaType: String = "bandwidth",
            val usedBandwidth: Long,
            val totalBandwidth: Long,
            val resetTime: Instant
        ) : QuotaException(
            message = "$message: ${formatBytes(usedBandwidth)}/${formatBytes(totalBandwidth)} (resets at $resetTime)",
            cause = cause,
            errorCode = "BANDWIDTH_QUOTA_EXCEEDED",
            quotaType = quotaType
        )
    }
    
    /**
     * Cache exceptions
     */
    sealed class CacheException(
        message: String,
        cause: Throwable? = null,
        errorCode: String,
        val cachePath: String
    ) : NetworkStorageException(message, cause, errorCode) {
        
        /**
         * Cache corruption detected
         */
        class Corruption(
            message: String = "Cache corruption detected",
            cause: Throwable? = null,
            cachePath: String,
            val checksumMismatch: Boolean = false,
            val expectedSize: Long? = null,
            val actualSize: Long? = null
        ) : CacheException(
            message = "$message: $cachePath${if (checksumMismatch) " (checksum mismatch)" else ""}${expectedSize?.let { " (expected size: ${formatBytes(it)})" } ?: ""}${actualSize?.let { " (actual size: ${formatBytes(it)})" } ?: ""}",
            cause = cause,
            errorCode = "CACHE_CORRUPTION",
            cachePath = cachePath
        )
        
        /**
         * Cache entry not found
         */
        class EntryNotFound(
            message: String = "Cache entry not found",
            cause: Throwable? = null,
            cachePath: String,
            val key: String
        ) : CacheException(
            message = "$message: $key in $cachePath",
            cause = cause,
            errorCode = "CACHE_ENTRY_NOT_FOUND",
            cachePath = cachePath
        )
    }
    
    /**
     * Generic network error
     */
    class GenericError(
        message: String,
        cause: Throwable? = null,
        errorCode: String = "GENERIC_ERROR",
        val operation: String? = null,
        val context: Map<String, Any> = emptyMap()
    ) : NetworkStorageException(message, cause, errorCode)
    
    /**
     * Conflict types for sync exceptions
     */
    enum class ConflictType {
        /**
         * Both remote and local have been modified
         */
        BOTH_MODIFIED,
        
        /**
         * Remote was modified while we had local changes
         */
        REMOTE_MODIFIED,
        
        /**
         * Local was modified while remote had changes
         */
        LOCAL_MODIFIED,
        
        /**
         * File was deleted on one side, modified on the other
         */
        DELETE_MODIFY_CONFLICT,
        
        /**
         * File was renamed differently on both sides
         */
        RENAME_CONFLICT,
        
        /**
         * Unknown conflict type
         */
        UNKNOWN
    }
    
    /**
     * Create a user-friendly error message
     */
    fun toUserMessage(): String {
        return when (this) {
            is ConnectionException.Timeout -> "Connection timed out. Please check your internet connection and try again."
            is ConnectionException.Authentication -> "Authentication failed. Please check your credentials and try again."
            is ConnectionException.SslError -> "Secure connection failed. Please check certificate settings."
            is ConnectionException.ServerUnreachable -> "Server is unreachable. Please check server address and try again."
            is ConnectionException.NetworkUnavailable -> "Network is unavailable. Please check your internet connection."
            is FileOperationException.NotFound -> "File not found: ${this.filePath}"
            is FileOperationException.PermissionDenied -> "Permission denied: ${this.filePath}"
            is FileOperationException.AlreadyExists -> "File already exists: ${this.filePath}"
            is FileOperationException.InsufficientSpace -> "Not enough space: ${this.filePath}"
            is FileOperationException.Locked -> "File is locked: ${this.filePath}"
            is ProtocolException.Unsupported -> "Unsupported protocol: ${this.protocol}"
            is SyncException.Conflict -> "Sync conflict detected: ${this.remotePath}"
            is QuotaException.Exceeded -> "Storage quota exceeded. Please free up space."
            is QuotaException.BandwidthExceeded -> "Bandwidth quota exceeded. Please try again later."
            is CacheException.Corruption -> "Cache corruption detected. Please clear cache and try again."
            is CacheException.EntryNotFound -> "Cache file not found. Please download again."
            else -> message ?: "Unknown error occurred"
        }
    }
    
    /**
     * Check if this error is retryable
     */
    fun isRetryable(): Boolean {
        return when (this) {
            is ConnectionException.Timeout,
            is ConnectionException.NetworkUnavailable,
            is ConnectionException.ServerUnreachable,
            is QuotaException.BandwidthExceeded -> true
            else -> false
        }
    }
    
    /**
     * Check if this error indicates a permanent failure
     */
    fun isPermanentFailure(): Boolean {
        return when (this) {
            is ConnectionException.Authentication,
            is FileOperationException.PermissionDenied,
            is ProtocolException.Unsupported,
            is ProtocolException.VersionMismatch,
            is QuotaException.Exceeded -> true
            else -> false
        }
    }
    
    /**
     * Get suggested action for the user
     */
    fun getSuggestedAction(): String? {
        return when (this) {
            is ConnectionException.Timeout -> "Check internet connection and try again"
            is ConnectionException.Authentication -> "Verify credentials and try again"
            is ConnectionException.SslError -> "Check SSL certificate settings"
            is FileOperationException.NotFound -> "Check file path and try again"
            is FileOperationException.PermissionDenied -> "Check file permissions"
            is FileOperationException.InsufficientSpace -> "Free up disk space"
            is SyncException.Conflict -> "Choose which version to keep"
            is QuotaException.Exceeded -> "Free up storage space"
            is CacheException.Corruption -> "Clear application cache"
            else -> null
        }
    }
    
    companion object {
        /**
         * Format bytes for human-readable display
         */
        private fun formatBytes(bytes: Long): String {
            return when {
                bytes < 1024 -> "${bytes}B"
                bytes < 1024 * 1024 -> "${bytes / 1024}KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
                else -> "${bytes / (1024 * 1024 * 1024)}GB"
            }
        }
        
        /**
         * Create from a generic throwable
         */
        fun fromThrowable(
            throwable: Throwable,
            operation: String? = null,
            filePath: String? = null,
            remotePath: String? = null
        ): NetworkStorageException {
            return when {
                throwable is NetworkStorageException -> throwable
                throwable.message?.contains("timeout", ignoreCase = true) == true ->
                    ConnectionException.Timeout(timeoutMs = 30000L, cause = throwable)
                throwable.message?.contains("authentication", ignoreCase = true) == true ->
                    ConnectionException.Authentication(cause = throwable, authType = "unknown", username = "unknown")
                throwable.message?.contains("permission", ignoreCase = true) == true ->
                    FileOperationException.PermissionDenied(
                        filePath = filePath ?: "unknown",
                        requiredPermission = "unknown",
                        cause = throwable
                    )
                throwable.message?.contains("quota", ignoreCase = true) == true ->
                    QuotaException.Exceeded(cause = throwable, usedSpace = 0L, totalQuota = 0L)
                else ->
                    GenericError(
                        message = throwable.message ?: "Unknown error",
                        cause = throwable,
                        operation = operation,
                        context = mapOf(
                            "filePath" to (filePath ?: ""),
                            "remotePath" to (remotePath ?: "")
                        )
                    )
            }
        }
    }
}