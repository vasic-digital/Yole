package digital.vasic.yole.network.common

import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

/**
 * Represents a network operation (upload, download, delete, etc.).
 * This data class tracks the state and progress of asynchronous network operations.
 */
data class NetworkOperation(
    /**
     * Unique identifier for this operation
     */
    val id: Long,
    
    /**
     * Type of operation being performed
     */
    val type: Type,
    
    /**
     * Remote path of the file/folder being operated on
     */
    val remotePath: String,
    
    /**
     * Local path of the file/folder being operated on (null for remote-only operations)
     */
    val localPath: String? = null,
    
    /**
     * Current status of the operation
     */
    val status: Status = Status.PENDING,
    
    /**
     * Progress of the operation (0.0 to 1.0)
     */
    val progress: Double = 0.0,
    
    /**
     * Size of the data being transferred in bytes
     */
    val totalSize: Long = 0L,
    
    /**
     * Bytes transferred so far
     */
    val bytesTransferred: Long = 0L,
    
    /**
     * When this operation was created
     */
    val createdAt: Instant,
    
    /**
     * When this operation was started (null if not started)
     */
    val startedAt: Instant? = null,
    
    /**
     * When this operation was completed (null if not completed)
     */
    val completedAt: Instant? = null,
    
    /**
     * Error message if the operation failed (null if no error)
     */
    val error: String? = null,
    
    /**
     * Number of retry attempts made
     */
    val retryCount: Int = 0,
    
    /**
     * Maximum retry attempts allowed
     */
    val maxRetries: Int = 3,
    
    /**
     * Priority of this operation (higher number = higher priority)
     */
    val priority: Int = 100,
    
    /**
     * Whether this operation can be paused
     */
    val canPause: Boolean = true,
    
    /**
     * Whether this operation can be cancelled
     */
    val canCancel: Boolean = true,
    
    /**
     * Whether this operation is currently paused
     */
    val isPaused: Boolean = false,
    
    /**
     * Estimated time remaining in milliseconds (null if unknown)
     */
    val estimatedTimeRemaining: Long? = null,
    
    /**
     * Transfer speed in bytes per second (null if unknown or not applicable)
     */
    val transferSpeed: Long? = null,
    
    /**
     * Additional operation metadata
     */
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Whether this operation is currently running
     */
    val isRunning: Boolean
        get() = status == Status.IN_PROGRESS && !isPaused
    
    /**
     * Whether this operation has completed successfully
     */
    val isCompleted: Boolean
        get() = status == Status.COMPLETED
    
    /**
     * Whether this operation has failed
     */
    val hasFailed: Boolean
        get() = status == Status.FAILED
    
    /**
     * Whether this operation can be retried
     */
    val canRetry: Boolean
        get() = hasFailed && retryCount < maxRetries
    
    /**
     * Whether this operation is pending
     */
    val isPending: Boolean
        get() = status == Status.PENDING
    
    /**
     * Progress as a percentage (0 to 100)
     */
    val progressPercentage: Int
        get() = (progress * 100).toInt()
    
    /**
     * Duration of the operation in milliseconds
     */
    val duration: Long?
        get() = when {
            completedAt != null && startedAt != null -> (completedAt - startedAt).inWholeMilliseconds
            startedAt != null -> (kotlinx.datetime.Clock.System.now() - startedAt).inWholeMilliseconds
            else -> null
        }
    
    /**
     * Create a copy with updated status
     */
    fun withStatus(status: Status, error: String? = null): NetworkOperation {
        val now = kotlinx.datetime.Clock.System.now()
        return copy(
            status = status,
            error = error,
            startedAt = if (status == Status.IN_PROGRESS && startedAt == null) now else startedAt,
            completedAt = if (status == Status.COMPLETED || status == Status.FAILED) now else completedAt
        )
    }
    
    /**
     * Create a copy with updated progress
     */
    fun withProgress(
        progress: Double, 
        bytesTransferred: Long? = null,
        transferSpeed: Long? = null,
        estimatedTimeRemaining: Long? = null
    ): NetworkOperation {
        return copy(
            progress = progress.coerceIn(0.0, 1.0),
            bytesTransferred = bytesTransferred ?: (totalSize * progress).toLong(),
            transferSpeed = transferSpeed,
            estimatedTimeRemaining = estimatedTimeRemaining
        )
    }
    
    /**
     * Create a copy with error information and increment retry count
     */
    fun withError(error: String): NetworkOperation {
        return copy(
            status = Status.FAILED,
            error = error,
            retryCount = retryCount + 1,
            completedAt = kotlinx.datetime.Clock.System.now()
        )
    }
    
    /**
     * Create a copy with pause status updated
     */
    fun withPauseStatus(isPaused: Boolean): NetworkOperation {
        return copy(isPaused = isPaused)
    }
    
    /**
     * Create a copy with updated priority
     */
    fun withPriority(priority: Int): NetworkOperation {
        return copy(priority = priority)
    }
    
    companion object {
        /**
         * Create a new error operation
         */
        fun error(
            id: Long,
            operationType: Type,
            remotePath: String,
            localPath: String? = null,
            error: String
        ): NetworkOperation {
            return NetworkOperation(
                id = id,
                type = operationType,
                remotePath = remotePath,
                localPath = localPath,
                status = Status.FAILED,
                error = error,
                createdAt = kotlinx.datetime.Clock.System.now(),
                startedAt = kotlinx.datetime.Clock.System.now(),
                completedAt = kotlinx.datetime.Clock.System.now()
            )
        }
        
        /**
         * Create a new upload operation
         */
        fun createUpload(
            id: String,
            localPath: String,
            remotePath: String,
            totalSize: Long = 0L,
            priority: Int = 100
        ): NetworkOperation {
            return NetworkOperation(
                id = id.hashCode().toLong(),
                type = Type.UPLOAD,
                remotePath = remotePath,
                localPath = localPath,
                totalSize = totalSize,
                createdAt = kotlinx.datetime.Clock.System.now(),
                priority = priority
            )
        }
        
        /**
         * Create a new download operation
         */
        fun createDownload(
            id: String,
            remotePath: String,
            localPath: String,
            totalSize: Long = 0L,
            priority: Int = 100
        ): NetworkOperation {
            return NetworkOperation(
                id = id.hashCode().toLong(),
                type = Type.DOWNLOAD,
                remotePath = remotePath,
                localPath = localPath,
                totalSize = totalSize,
                createdAt = kotlinx.datetime.Clock.System.now(),
                priority = priority
            )
        }
        
        /**
         * Create a new delete operation
         */
        fun createDelete(
            id: Long,
            remotePath: String,
            priority: Int = 100
        ): NetworkOperation {
            return NetworkOperation(
                id = id,
                type = Type.DELETE,
                remotePath = remotePath,
                canPause = false,
                createdAt = kotlinx.datetime.Clock.System.now(),
                priority = priority
            )
        }
        
        /**
         * Create a new create folder operation
         */
        fun createFolder(
            id: Long,
            remotePath: String,
            priority: Int = 100
        ): NetworkOperation {
            return NetworkOperation(
                id = id,
                type = Type.CREATE_FOLDER,
                remotePath = remotePath,
                canPause = false,
                createdAt = kotlinx.datetime.Clock.System.now(),
                priority = priority
            )
        }
        
        /**
         * Create a new sync operation
         */
        fun createSync(
            id: String,
            remotePath: String,
            priority: Int = 100
        ): NetworkOperation {
            return NetworkOperation(
                id = id.hashCode().toLong(),
                type = Type.SYNC,
                remotePath = remotePath,
                canPause = true,
                createdAt = kotlinx.datetime.Clock.System.now(),
                priority = priority
            )
        }
        
        /**
         * Create a mock operation for testing
         */
        fun mock(
            type: Type = Type.UPLOAD,
            status: Status = Status.IN_PROGRESS,
            progress: Double = 0.5
        ): NetworkOperation {
            return NetworkOperation(
                id = 1L,
                type = type,
                remotePath = "/test.txt",
                localPath = "/local/test.txt",
                status = status,
                progress = progress,
                totalSize = 1024L,
                bytesTransferred = (1024L * progress).toLong(),
                createdAt = kotlinx.datetime.Clock.System.now().minus(1.minutes),
                startedAt = kotlinx.datetime.Clock.System.now().minus(1.minutes)
            )
        }
    }
    
    /**
     * Types of network operations
     */
    enum class Type {
        UPLOAD,
        DOWNLOAD,
        DELETE,
        CREATE_FOLDER,
        RENAME,
        COPY,
        MOVE,
        SYNC
    }
    
    /**
     * Status of network operations
     */
    enum class Status {
        PENDING,
        IN_PROGRESS,
        PAUSED,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}