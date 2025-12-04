# Network Storage API Contract

**Date**: 2025-12-04  
**Feature**: Network Protocols Implementation  
**Purpose**: Define unified API for all network storage protocols

## Core Interface

### NetworkStorageService

```kotlin
interface NetworkStorageService {
    // Connection Management
    suspend fun testConnection(config: StorageConfig): Result<Boolean>
    suspend fun authenticate(config: StorageConfig): Result<AuthResult>
    
    // File Operations
    suspend fun listFiles(storageId: String, path: String): Result<List<NetworkDocument>>
    suspend fun getDocument(storageId: String, path: String): Result<NetworkDocument>
    suspend fun downloadDocument(document: NetworkDocument, localPath: String): Result<NetworkOperation>
    suspend fun uploadDocument(storageId: String, localPath: String, remotePath: String): Result<NetworkOperation>
    suspend fun deleteDocument(storageId: String, path: String): Result<NetworkOperation>
    suspend fun moveDocument(storageId: String, fromPath: String, toPath: String): Result<NetworkOperation>
    suspend fun copyDocument(storageId: String, fromPath: String, toPath: String): Result<NetworkOperation>
    suspend fun createFolder(storageId: String, path: String): Result<NetworkDocument>
    
    // Sync Operations
    suspend fun syncFolder(storageId: String, path: String): Result<SyncResult>
    suspend fun getSyncStatus(documentId: String): Result<DocumentSyncStatus>
    
    // Operation Management
    suspend fun getOperation(operationId: String): Result<NetworkOperation>
    suspend fun cancelOperation(operationId: String): Result<Boolean>
    suspend fun pauseOperation(operationId: String): Result<Boolean>
    suspend fun resumeOperation(operationId: String): Result<Boolean>
    fun getOperationStream(): Flow<NetworkOperation>
}

data class AuthResult(
    val success: Boolean,
    val token: String? = null,
    val refreshToken: String? = null,
    val expiresAt: Instant? = null,
    val error: String? = null
)

data class SyncResult(
    val success: Boolean,
    val syncedDocuments: List<String>,
    val conflicts: List<DocumentConflict>,
    val errors: List<SyncError>
)

data class DocumentConflict(
    val documentId: String,
    val path: String,
    val localVersion: String,
    val remoteVersion: String,
    val conflictType: ConflictType
)

enum class ConflictType {
    BOTH_MODIFIED,    // Both local and remote modified
    DELETED_MODIFIED, // One deleted, other modified
    RENAME_CONFLICT   // Same name created independently
}

data class SyncError(
    val path: String,
    val errorType: ErrorType,
    val message: String,
    val retryable: Boolean
)

enum class ErrorType {
    NETWORK_ERROR,       // Network connectivity issues
    AUTHENTICATION_ERROR, // Invalid credentials
    PERMISSION_ERROR,    // Insufficient permissions
    NOT_FOUND,          // File/folder not found
    SERVER_ERROR,        // Server-side error
    STORAGE_FULL,        // Quota exceeded
    TIMEOUT,            // Operation timed out
    UNKNOWN             // Unclassified error
}
```

## Protocol-Specific Interfaces

### WebDAV Service

```kotlin
interface WebDavService : NetworkStorageService {
    // WebDAV-specific operations
    suspend fun getProperties(storageId: String, path: String): Result<WebDavProperties>
    suspend fun setProperties(storageId: String, path: String, properties: Map<String, String>): Result<Boolean>
    suspend fun lockDocument(storageId: String, path: String): Result<WebDavLock>
    suspend fun unlockDocument(storageId: String, path: String, lockToken: String): Result<Boolean>
}

data class WebDavProperties(
    val creationDate: Instant,
    val lastModified: Instant,
    val contentLength: Long,
    val contentType: String?,
    val etag: String?,
    val customProperties: Map<String, String>
)

data class WebDavLock(
    val token: String,
    val scope: String,
    val type: LockType,
    val depth: LockDepth,
    val owner: String,
    val timeout: Duration,
    val createdAt: Instant
)

enum class LockType { EXCLUSIVE, SHARED }
enum class LockDepth { ZERO, INFINITY }
```

### FTP/SFTP Service

```kotlin
interface FtpService : NetworkStorageService {
    // FTP-specific operations
    suspend fun changeWorkingDirectory(storageId: String, path: String): Result<Boolean>
    suspend fun getWorkingDirectory(storageId: String): Result<String>
    suspend fun setTransferMode(storageId: String, mode: TransferMode): Result<Boolean>
    suspend fun getFileInfo(storageId: String, path: String): Result<FtpFileInfo>
}

data class FtpFileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val permissions: String,
    val owner: String,
    val group: String,
    val modifiedAt: Instant
)

enum class TransferMode { ASCII, BINARY }
```

### SMB Service

```kotlin
interface SmbService : NetworkStorageService {
    // SMB-specific operations
    suspend fun listShares(storageId: String): Result<List<SmbShare>>
    suspend fun getShareInfo(storageId: String, shareName: String): Result<SmbShareInfo>
    suspend fun setFileAttributes(storageId: String, path: String, attributes: FileAttributes): Result<Boolean>
}

data class SmbShare(
    val name: String,
    val type: ShareType,
    val comment: String?,
    val permissions: SharePermissions
)

data class SmbShareInfo(
    val name: String,
    val path: String,
    val totalSize: Long,
    val usedSpace: Long,
    val availableSpace: Long
)

data class FileAttributes(
    val readOnly: Boolean = false,
    val hidden: Boolean = false,
    val archived: Boolean = false,
    val system: Boolean = false
)

enum class ShareType { DISK, PRINTER, PIPE, COMM }
enum class SharePermissions { READ, WRITE, FULL_CONTROL }
```

### Cloud Storage Service

```kotlin
interface CloudStorageService : NetworkStorageService {
    // Cloud-specific operations
    suspend fun getAccountInfo(): Result<CloudAccountInfo>
    suspend fun getQuotaInfo(): Result<CloudQuotaInfo>
    suspend fun getShareLink(storageId: String, path: String, permissions: SharePermissions): Result<String>
    suspend fun getRevisionHistory(storageId: String, path: String): Result<List<DocumentRevision>>
    suspend fun restoreRevision(storageId: String, path: String, revisionId: String): Result<Boolean>
}

data class CloudAccountInfo(
    val userId: String,
    val email: String,
    val displayName: String,
    val accountType: AccountType,
    val verified: Boolean
)

data class CloudQuotaInfo(
    val totalSpace: Long,
    val usedSpace: Long,
    val availableSpace: Long,
    val storageBreakdown: Map<String, Long>
)

data class DocumentRevision(
    val id: String,
    val modifiedAt: Instant,
    val size: Long,
    val modifiedBy: String,
    val comment: String?
)

enum class AccountType { FREE, PREMIUM, BUSINESS, ENTERPRISE }
```

## Event System

### Network Events

```kotlin
sealed class NetworkEvent {
    data class StorageConnected(val storageId: String) : NetworkEvent()
    data class StorageDisconnected(val storageId: String, val error: String?) : NetworkEvent()
    data class OperationStarted(val operation: NetworkOperation) : NetworkEvent()
    data class OperationProgress(val operationId: String, val progress: Float) : NetworkEvent()
    data class OperationCompleted(val operation: NetworkOperation) : NetworkEvent()
    data class OperationFailed(val operation: NetworkOperation, val error: String) : NetworkEvent()
    data class SyncStarted(val storageId: String, val path: String) : NetworkEvent()
    data class SyncCompleted(val storageId: String, val result: SyncResult) : NetworkEvent()
    data class ConflictDetected(val conflict: DocumentConflict) : NetworkEvent()
    data class ConflictResolved(val documentId: String, val resolution: ConflictResolution) : NetworkEvent()
}

interface NetworkEventEmitter {
    fun emitEvent(event: NetworkEvent)
}

interface NetworkEventListener {
    fun onEvent(event: NetworkEvent)
}
```

## Configuration APIs

### Storage Configuration

```kotlin
interface StorageConfigurationService {
    suspend fun addStorage(storage: NetworkStorage): Result<String>
    suspend fun updateStorage(storageId: String, updates: NetworkStorage): Result<Boolean>
    suspend fun removeStorage(storageId: String): Result<Boolean>
    suspend fun getStorage(storageId: String): Result<NetworkStorage>
    suspend fun listStorages(): Result<List<NetworkStorage>>
    suspend fun setDefaultStorage(storageId: String): Result<Boolean>
    suspend fun getDefaultStorage(): Result<NetworkStorage?>
}
```

### Cache Configuration

```kotlin
interface CacheConfigurationService {
    suspend fun setCacheSize(sizeBytes: Long): Result<Boolean>
    suspend fun getCacheSize(): Result<Long>
    suspend fun setCachePolicy(policy: CachePolicy): Result<Boolean>
    suspend fun getCachePolicy(): Result<CachePolicy>
    suspend fun clearCache(): Result<Boolean>
    suspend fun getCacheUsage(): Result<CacheUsage>
}

data class CacheUsage(
    val totalSpace: Long,
    val usedSpace: Long,
    val documentCount: Int,
    val hitRate: Float
)

enum class CachePolicy {
    LRU,               // Least Recently Used
    LFU,               // Least Frequently Used
    FIFO,              // First In, First Out
    SIZE_BASED,        // Evict largest files first
    TIME_BASED         // Evict oldest files first
}
```

## Error Handling

### Standardized Error Types

```kotlin
sealed class NetworkStorageError : Exception() {
    data class ConnectionError(val message: String, val cause: Throwable?) : NetworkStorageError()
    data class AuthenticationError(val message: String) : NetworkStorageError()
    data class PermissionError(val message: String) : NetworkStorageError()
    data class FileNotFoundError(val path: String) : NetworkStorageError()
    data class QuotaExceededError(val message: String) : NetworkStorageError()
    data class ServerError(val code: Int, val message: String) : NetworkStorageError()
    data class TimeoutError(val operation: String, val timeout: Duration) : NetworkStorageError()
    data class ValidationError(val field: String, val message: String) : NetworkStorageError()
    data class UnknownError(val message: String, val cause: Throwable?) : NetworkStorageError()
}
```

## Testing Contracts

### Mock Service Interface

```kotlin
interface MockNetworkStorageService : NetworkStorageService {
    // Test configuration methods
    fun setConnectionResult(success: Boolean, delay: Duration = 0.seconds)
    fun setDownloadResult(success: Boolean, size: Long = 1024)
    fun setUploadResult(success: Boolean, delay: Duration = 1.seconds)
    fun simulateConflict(documentId: String, conflictType: ConflictType)
    fun simulateNetworkError()
    fun getOperationHistory(): List<NetworkOperation>
    
    // Test verification methods
    fun verifyConnectionAttempted(): Boolean
    fun verifyDownloadAttempted(path: String): Boolean
    fun verifyUploadAttempted(path: String): Boolean
    fun verifyCredentialsEncrypted(): Boolean
}
```

## Integration Points

### Yole Integration

```kotlin
interface YoleNetworkIntegration {
    // Document format integration
    suspend fun detectRemoteDocumentFormat(document: NetworkDocument): Result<TextFormat>
    
    // File browser integration
    suspend fun getNetworkStorageNodes(): Result<List<FileBrowserNode>>
    
    // Document editor integration
    suspend fun openRemoteDocument(document: NetworkDocument): Result<Document>
    suspend fun saveRemoteDocument(document: Document): Result<Boolean>
    
    // Settings integration
    suspend fun getNetworkSettings(): Result<NetworkSettings>
    suspend fun updateNetworkSettings(settings: NetworkSettings): Result<Boolean>
}

data class FileBrowserNode(
    val id: String,
    val name: String,
    val type: NodeType,
    val storageId: String,
    val path: String,
    val icon: String?,
    val canRead: Boolean,
    val canWrite: Boolean
)

enum class NodeType { STORAGE, FOLDER, FILE }

data class NetworkSettings(
    val enableAutoSync: Boolean = true,
    val syncInterval: Duration = 15.minutes,
    val cacheSize: Long = 100 * 1024 * 1024, // 100MB
    val connectionTimeout: Duration = 30.seconds,
    val retryAttempts: Int = 3,
    val enableNotifications: Boolean = true
)
```