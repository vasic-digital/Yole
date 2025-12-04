package digital.vasic.yole.network.common

import kotlinx.datetime.Instant

/**
 * Represents the synchronization status of a document or storage.
 * This enum tracks the state of data synchronization between local and remote storage.
 */
enum class SyncStatus {
    /**
     * Document is synchronized and up to date
     */
    SYNCED,
    
    /**
     * Document has local changes that need to be uploaded
     */
    PENDING_UPLOAD,
    
    /**
     * Document has remote changes that need to be downloaded
     */
    PENDING_DOWNLOAD,
    
    /**
     * Document is currently being synchronized
     */
    SYNCING,
    
    /**
     * Synchronization failed due to an error
     */
    SYNC_ERROR,
    
    /**
     * Document is not synchronized (offline mode or disabled)
     */
    NOT_SYNCED,
    
    /**
     * Document is being queued for synchronization
     */
    QUEUED,
    
    /**
     * Document conflicts exist that need resolution
     */
    CONFLICT,
    
    /**
     * Document is being uploaded
     */
    UPLOADING,
    
    /**
     * Document is being downloaded
     */
    DOWNLOADING
}

/**
 * Represents detailed synchronization status and metadata for a document.
 * This data class provides additional context about the synchronization process.
 */
data class DocumentSyncStatus(
    /**
     * Current synchronization status
     */
    val status: SyncStatus,
    
    /**
     * Timestamp of last successful synchronization
     */
    val lastSyncTime: Instant? = null,
    
    /**
     * Timestamp of next scheduled synchronization
     */
    val nextSyncTime: Instant? = null,
    
    /**
     * Synchronization error message (null if no error)
     */
    val errorMessage: String? = null,
    
    /**
     * Number of retry attempts made
     */
    val retryCount: Int = 0,
    
    /**
     * Maximum retry attempts allowed
     */
    val maxRetries: Int = 3,
    
    /**
     * Progress of current synchronization (0.0 to 1.0)
     */
    val progress: Double = 0.0,
    
    /**
     * Estimated time remaining for synchronization (null if unknown)
     */
    val estimatedTimeRemaining: Long? = null,
    
    /**
     * Size of data being synchronized in bytes
     */
    val dataSize: Long = 0L,
    
    /**
     * Bytes transferred so far
     */
    val bytesTransferred: Long = 0L,
    
    /**
     * Whether synchronization is automatic
     */
    val isAutomatic: Boolean = true,
    
    /**
     * Whether the document is available offline
     */
    val isAvailableOffline: Boolean = false,
    
    /**
     * Whether the document has conflicts that need resolution
     */
    val hasConflicts: Boolean = false,
    
    /**
     * Conflict resolution strategy
     */
    val conflictResolution: ConflictResolution? = null,
    
    /**
     * Additional synchronization metadata
     */
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Whether synchronization is currently in progress
     */
    val isSyncing: Boolean
        get() = status == SyncStatus.SYNCING || 
               status == SyncStatus.UPLOADING || 
               status == SyncStatus.DOWNLOADING
    
    /**
     * Whether synchronization has failed
     */
    val hasFailed: Boolean
        get() = status == SyncStatus.SYNC_ERROR
    
    /**
     * Whether synchronization can be retried
     */
    val canRetry: Boolean
        get() = hasFailed && retryCount < maxRetries
    
    /**
     * Whether synchronization is pending
     */
    val isPending: Boolean
        get() = status == SyncStatus.PENDING_UPLOAD || 
               status == SyncStatus.PENDING_DOWNLOAD || 
               status == SyncStatus.QUEUED
    
    /**
     * Transfer progress as a percentage (0 to 100)
     */
    val progressPercentage: Int
        get() = (progress * 100).toInt()
    
    /**
     * Transfer speed in bytes per second (if available)
     */
    val transferSpeed: Long?
        get() = if (isSyncing && estimatedTimeRemaining != null && bytesTransferred > 0) {
            bytesTransferred / estimatedTimeRemaining
        } else null
    
    /**
     * Create a copy with updated status
     */
    fun withStatus(status: SyncStatus): DocumentSyncStatus {
        return copy(status = status)
    }
    
    /**
     * Create a copy with updated progress
     */
    fun withProgress(progress: Double, bytesTransferred: Long? = null): DocumentSyncStatus {
        return copy(
            progress = progress.coerceIn(0.0, 1.0),
            bytesTransferred = bytesTransferred ?: (dataSize * progress).toLong()
        )
    }
    
    /**
     * Create a copy with error information
     */
    fun withError(errorMessage: String, incrementRetry: Boolean = true): DocumentSyncStatus {
        return copy(
            status = SyncStatus.SYNC_ERROR,
            errorMessage = errorMessage,
            retryCount = if (incrementRetry) retryCount + 1 else retryCount
        )
    }
    
    /**
     * Create a copy with successful sync information
     */
    fun withSuccess(): DocumentSyncStatus {
        return copy(
            status = SyncStatus.SYNCED,
            lastSyncTime = kotlinx.datetime.Clock.System.now(),
            errorMessage = null,
            retryCount = 0,
            progress = 1.0,
            bytesTransferred = dataSize,
            isAvailableOffline = true
        )
    }
    
    companion object {
        /**
         * Create a fresh sync status for a new document
         */
        fun forNewDocument(isAutomatic: Boolean = true): DocumentSyncStatus {
            return DocumentSyncStatus(
                status = SyncStatus.PENDING_UPLOAD,
                isAutomatic = isAutomatic,
                isAvailableOffline = false
            )
        }
        
        /**
         * Create a sync status for an existing document
         */
        fun forExistingDocument(lastSyncTime: Instant? = null): DocumentSyncStatus {
            return DocumentSyncStatus(
                status = SyncStatus.SYNCED,
                lastSyncTime = lastSyncTime,
                isAvailableOffline = true
            )
        }
    }
}

/**
 * Represents conflict resolution strategies for synchronization conflicts.
 */
enum class ConflictResolution {
    /**
     * Use local version and overwrite remote
     */
    LOCAL_WINS,
    
    /**
     * Use remote version and overwrite local
     */
    REMOTE_WINS,
    
    /**
     * Keep both versions (create a copy)
     */
    KEEP_BOTH,
    
    /**
     * Manually resolve the conflict
     */
    MANUAL,
    
    /**
     * Skip synchronization until manually resolved
     */
    SKIP
}