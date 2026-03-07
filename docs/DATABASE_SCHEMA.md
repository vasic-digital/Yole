<!--
SPDX-FileCopyrightText: 2024-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Database Schema Documentation

This document describes the entity types, fields, relationships, and query patterns used by Yole's network storage metadata database (`NetworkStorageDatabase`), implemented in the Database-KMP module (`digital.vasic.database`).

---

## Overview

Yole uses a local database to persist metadata about network storage operations, cached files, and synchronization status. The database interface is defined by `NetworkStorageDatabase` and implemented using platform-specific storage backends.

The database stores **metadata only** -- actual file content is stored on disk in the local cache directory. The database tracks where those files came from, when they were last synced, and what operations are pending.

---

## Entity Types

### NetworkStorage

Represents a configured storage provider (e.g., a specific Dropbox account or WebDAV server).

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | `String` | Yes | Unique identifier (e.g., `"dropbox_My Dropbox"`) |
| `name` | `String` | Yes | Human-readable name |
| `type` | `StorageType` | Yes | Provider type: `WEBDAV`, `FTP`, `SFTP`, `SMB`, `GOOGLE_DRIVE`, `DROPBOX`, `ONEDRIVE`, `GIT` |
| `location` | `String` | Yes | Endpoint URL or path |
| `totalSpace` | `Long?` | No | Total storage quota in bytes |
| `usedSpace` | `Long?` | No | Used space in bytes |
| `isOnline` | `Boolean` | No | Whether currently connected (default: `false`) |
| `lastSync` | `Instant?` | No | Timestamp of last synchronization |
| `metadata` | `Map<String, String>` | No | Additional key-value metadata |
| `isEnabled` | `Boolean` | No | Whether this storage is active (default: `true`) |
| `priority` | `Int` | No | Display/operation priority (lower = higher priority, default: `100`) |
| `supportsFolders` | `Boolean` | No | Whether folders are supported (default: `true`) |
| `supportsMetadata` | `Boolean` | No | Whether file metadata is supported (default: `true`) |
| `maxFileSize` | `Long?` | No | Maximum supported file size in bytes |
| `supportedExtensions` | `List<String>` | No | Supported file extensions (empty = all) |

**Computed properties**:
- `availableSpace` -- `totalSpace - usedSpace` (null if either is null)
- `usagePercentage` -- `usedSpace / totalSpace` as a Double (0.0 to 1.0)
- `isFull` -- `availableSpace == 0`
- `isLowOnSpace` -- `usagePercentage > 0.9`

---

### NetworkDocument

Represents a file or folder stored on a remote storage provider.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | `String` | Yes | Unique identifier within the storage (often the file path) |
| `name` | `String` | Yes | Human-readable file or folder name |
| `path` | `String` | No | Full path within the storage (default: `""`) |
| `isFolder` | `Boolean` | No | Whether this is a folder (default: `false`) |
| `size` | `Long` | No | File size in bytes, 0 for folders (default: `0`) |
| `lastModified` | `Instant?` | No | Last modification timestamp |
| `syncStatus` | `SyncStatus` | No | Current sync state (default: `UNKNOWN`) |
| `documentId` | `String?` | No | Associated local document ID |
| `contentType` | `String?` | No | MIME type (null for folders) |
| `extension` | `String` | Auto | File extension (derived from `name`) |
| `parentPath` | `String` | Auto | Parent directory path (derived from `path`) |
| `isReadOnly` | `Boolean` | No | Whether the document is read-only (default: `false`) |
| `isHidden` | `Boolean` | No | Whether the document is hidden (default: `false`) |
| `metadata` | `Map<String, String>` | No | Additional key-value metadata |
| `thumbnails` | `List<String>` | No | Thumbnail URLs or paths |
| `tags` | `List<String>` | No | Tags or labels |
| `owner` | `String?` | No | Document owner |
| `permissions` | `Set<DocumentPermission>` | No | Permission set |
| `storageId` | `String` | No | Parent storage ID (default: `""`) |
| `author` | `String?` | No | Document author |

**Computed properties**:
- `isTextFile` -- true if extension is in the TEXT_EXTENSIONS set
- `isImageFile` -- true if extension is in the IMAGE_EXTENSIONS set
- `isPdfFile` -- true if extension is `.pdf`
- `isPreviewable` -- `isTextFile || isImageFile || isPdfFile`
- `isEditable` -- `isTextFile && !isReadOnly`
- `isSyncing` -- `syncStatus == SYNCING`
- `hasPendingChanges` -- `syncStatus == PENDING_UPLOAD`
- `isAvailableOffline` -- `syncStatus == SYNCED`
- `formattedSize` -- human-readable size string (e.g., "1KB", "5MB")

**DocumentPermission enum values**: `READ`, `WRITE`, `DELETE`, `EXECUTE`, `SHARE`

---

### CacheEntry

Represents a locally cached copy of a remote document for offline access.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | `String` | Yes | Unique cache entry identifier |
| `remoteDocumentId` | `String` | Yes | Remote document ID this entry caches |
| `localPath` | `String` | Yes | Local file path where cached content is stored |
| `remotePath` | `String` | Yes | Remote path of the original document |
| `size` | `Long` | Yes | Cached file size in bytes |
| `createdAt` | `Instant` | Yes | When the cache entry was created |
| `lastAccessed` | `Instant` | Yes | When the cache entry was last accessed |
| `lastModified` | `Instant` | Yes | When the cached content was last modified |
| `expiresAt` | `Instant?` | No | Expiration timestamp (null = never expires) |
| `isValid` | `Boolean` | Auto | Whether the entry is not expired |
| `isPinned` | `Boolean` | No | Whether the entry is protected from eviction (default: `false`) |
| `isInUse` | `Boolean` | No | Whether the entry is currently open (default: `false`) |
| `accessCount` | `Int` | No | Number of times accessed (default: `0`) |
| `contentType` | `String?` | No | MIME type of cached content |
| `checksum` | `String?` | No | Content hash for integrity verification |
| `compression` | `String?` | No | Compression algorithm (null = uncompressed) |
| `originalSize` | `Long?` | No | Size before compression |
| `priority` | `Int` | No | Eviction priority (higher = less likely to be evicted, default: `100`) |
| `metadata` | `Map<String, String>` | No | Additional key-value metadata |
| `tags` | `List<String>` | No | Tags for categorization |

**Computed properties**:
- `isExpired` -- `!isValid`
- `canBeEvicted` -- `!isPinned && !isInUse`
- `compressionRatio` -- `size / originalSize` (null if uncompressed)
- `age` -- milliseconds since creation
- `timeSinceLastAccess` -- milliseconds since last access
- `accessFrequency` -- accesses per day (approximate)

---

### NetworkOperation

Represents an in-progress or completed network operation (upload, download, sync, etc.).

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | `Long` | Yes | Unique operation identifier |
| `type` | `Type` | Yes | Operation type (see enum below) |
| `remotePath` | `String` | Yes | Remote file path |
| `localPath` | `String?` | No | Local file path (null for remote-only operations) |
| `status` | `Status` | No | Operation status (default: `PENDING`) |
| `progress` | `Double` | No | Progress 0.0 to 1.0 (default: `0.0`) |
| `totalSize` | `Long` | No | Total bytes to transfer (default: `0`) |
| `bytesTransferred` | `Long` | No | Bytes transferred so far (default: `0`) |
| `createdAt` | `Instant` | Yes | When the operation was created |
| `startedAt` | `Instant?` | No | When the operation started executing |
| `completedAt` | `Instant?` | No | When the operation completed |
| `error` | `String?` | No | Error message (null if no error) |
| `retryCount` | `Int` | No | Number of retry attempts (default: `0`) |
| `maxRetries` | `Int` | No | Maximum retry attempts (default: `3`) |
| `priority` | `Int` | No | Operation priority (default: `100`) |
| `canPause` | `Boolean` | No | Whether the operation supports pausing (default: `true`) |
| `canCancel` | `Boolean` | No | Whether the operation supports cancellation (default: `true`) |
| `isPaused` | `Boolean` | No | Whether the operation is currently paused (default: `false`) |
| `estimatedTimeRemaining` | `Long?` | No | Estimated time remaining in milliseconds |
| `transferSpeed` | `Long?` | No | Transfer speed in bytes per second |
| `metadata` | `Map<String, String>` | No | Additional key-value metadata |

**Operation Type enum**: `UPLOAD`, `DOWNLOAD`, `DELETE`, `CREATE_FOLDER`, `RENAME`, `COPY`, `MOVE`, `SYNC`

**Operation Status enum**: `PENDING`, `IN_PROGRESS`, `PAUSED`, `COMPLETED`, `FAILED`, `CANCELLED`

**Computed properties**:
- `isRunning` -- `status == IN_PROGRESS && !isPaused`
- `isCompleted` -- `status == COMPLETED`
- `hasFailed` -- `status == FAILED`
- `canRetry` -- `hasFailed && retryCount < maxRetries`
- `isPending` -- `status == PENDING`
- `progressPercentage` -- `(progress * 100).toInt()`
- `duration` -- milliseconds elapsed since `startedAt`

---

### SyncStatus

Enumeration tracking the synchronization state of a document.

| Value | Description |
|-------|-------------|
| `UNKNOWN` | Status has not been determined |
| `SYNCED` | Document is synchronized and up to date |
| `PENDING_UPLOAD` | Local changes need to be uploaded |
| `PENDING_DOWNLOAD` | Remote changes need to be downloaded |
| `SYNCING` | Currently being synchronized |
| `SYNC_ERROR` | Synchronization failed |
| `NOT_SYNCED` | Not being synchronized (offline or disabled) |
| `QUEUED` | Queued for synchronization |
| `CONFLICT` | Conflict exists that needs resolution |
| `UPLOADING` | Currently uploading |
| `DOWNLOADING` | Currently downloading |

---

### DocumentSyncStatus

Detailed synchronization metadata for a document.

| Field | Type | Description |
|-------|------|-------------|
| `status` | `SyncStatus` | Current sync state |
| `lastSyncTime` | `Instant?` | Last successful sync timestamp |
| `nextSyncTime` | `Instant?` | Next scheduled sync timestamp |
| `errorMessage` | `String?` | Error message if sync failed |
| `retryCount` | `Int` | Number of retry attempts |
| `maxRetries` | `Int` | Maximum retry attempts (default: 3) |
| `progress` | `Double` | Current sync progress (0.0 to 1.0) |
| `estimatedTimeRemaining` | `Long?` | Estimated time remaining in ms |
| `dataSize` | `Long` | Size of data being synced |
| `bytesTransferred` | `Long` | Bytes transferred so far |
| `isAutomatic` | `Boolean` | Whether sync is automatic |
| `isAvailableOffline` | `Boolean` | Whether the document is available offline |
| `hasConflicts` | `Boolean` | Whether conflicts exist |
| `conflictResolution` | `ConflictResolution?` | Conflict resolution strategy |

**ConflictResolution enum**: `LOCAL_WINS`, `REMOTE_WINS`, `KEEP_BOTH`, `MANUAL`, `SKIP`

---

## Relationships

```
NetworkStorage (1) --- (*) NetworkDocument
     |                         |
     |                         | (via remoteDocumentId)
     |                         |
     +--- (*) CacheEntry ------+
     |
     +--- (*) NetworkOperation
     |
     +--- (*) SyncStatus (keyed by remotePath)
```

- A **NetworkStorage** has many **NetworkDocuments** (linked by `storageId`)
- A **NetworkDocument** may have one or more **CacheEntries** (linked by `remoteDocumentId`)
- A **NetworkStorage** has many **NetworkOperations** (linked by `remotePath` prefix)
- **SyncStatus** entries are keyed by `remotePath` and associated with a storage context

---

## Database Operations

### Storage Operations

| Method | Description |
|--------|-------------|
| `insertStorage(storage)` | Insert a new storage configuration |
| `updateStorage(storage)` | Update an existing storage configuration |
| `getStorage(id)` | Retrieve a storage by ID |
| `getAllStorage()` | List all storage configurations |
| `deleteStorage(id)` | Remove a storage configuration |

### Document Operations

| Method | Description |
|--------|-------------|
| `insertDocument(document)` | Insert a new document record |
| `updateDocument(document)` | Update an existing document record |
| `getDocument(id)` | Retrieve a document by ID |
| `getDocumentsByStorage(storageId)` | List all documents for a storage |
| `getDocumentsByPath(path)` | List all documents at a path |
| `deleteDocument(id)` | Remove a document record |
| `observeDocumentsByStorage(storageId)` | Reactive flow of documents for a storage |

### Cache Operations

| Method | Description |
|--------|-------------|
| `insertCacheEntry(entry)` | Insert a new cache entry |
| `updateCacheEntry(entry)` | Update an existing cache entry |
| `getCacheEntry(id)` | Retrieve a cache entry by ID |
| `getCacheEntriesByDocument(documentId)` | List cache entries for a document |
| `getAllCacheEntries()` | List all cache entries |
| `deleteCacheEntry(id)` | Remove a cache entry |
| `deleteExpiredCacheEntries()` | Remove all expired entries (returns count) |
| `getCacheUsage()` | Total cache size in bytes |

### Operation Tracking

| Method | Description |
|--------|-------------|
| `insertOperation(operation)` | Insert a new operation record |
| `updateOperation(operation)` | Update an existing operation record |
| `getOperation(id)` | Retrieve an operation by ID |
| `getActiveOperations()` | List all non-completed operations |
| `deleteOperation(id)` | Remove an operation record |
| `clearCompletedOperations()` | Remove all completed operations (returns count) |

### Sync Status

| Method | Description |
|--------|-------------|
| `updateSyncStatus(remotePath, status)` | Set sync status for a path |
| `getSyncStatus(remotePath)` | Get sync status for a path |
| `getAllSyncStatus()` | Get all sync status entries as a map |
| `deleteSyncStatus(remotePath)` | Remove sync status for a path |

### Maintenance

| Method | Description |
|--------|-------------|
| `initialize()` | Initialize the database (create tables if needed) |
| `close()` | Close the database connection |
| `clearAll()` | Remove all data from all tables |
| `vacuum()` | Compact the database file |

---

## Query Patterns

### List documents for a specific storage

```kotlin
val documents = database.getDocumentsByStorage("dropbox_My Dropbox").getOrThrow()
```

### Find all documents at a specific path

```kotlin
val documents = database.getDocumentsByPath("/Notes").getOrThrow()
```

### Observe documents reactively (for UI updates)

```kotlin
database.observeDocumentsByStorage("dropbox_My Dropbox")
    .collect { documents ->
        updateUI(documents)
    }
```

### Clean up expired cache entries

```kotlin
val removed = database.deleteExpiredCacheEntries().getOrThrow()
println("Removed $removed expired cache entries")
```

### Check total cache usage

```kotlin
val bytes = database.getCacheUsage().getOrThrow()
if (bytes > MAX_CACHE_SIZE) {
    database.deleteExpiredCacheEntries()
}
```

### Get all pending sync operations

```kotlin
val allStatus = database.getAllSyncStatus().getOrThrow()
val pending = allStatus.filter { (_, status) ->
    status == SyncStatus.PENDING_UPLOAD || status == SyncStatus.PENDING_DOWNLOAD
}
```

---

## Migration Notes

### Schema Versioning

The database schema is defined by the data classes listed above. Schema migrations are handled by the platform-specific database implementation:

- **Android**: Room or SQLDelight with migration scripts
- **Desktop**: SQLDelight with JVM driver
- **iOS**: SQLDelight with Native driver
- **Web**: In-memory or IndexedDB

### Adding New Fields

When a new field is added to an entity data class with a default value, existing database records can be migrated by reading them with the default value applied. Non-nullable fields without defaults require a migration script that populates the new column.

### Data Integrity

All database operations return `Result<T>`, allowing the caller to handle failures gracefully. The `vacuum()` method compacts the database file and checks for corruption.

---

## Related Documentation

- [Architecture Guide](../ARCHITECTURE.md) -- System design overview
- [Performance Tuning](PERFORMANCE_TUNING.md) -- Optimizing database operations
- [Cloud Storage Overview](user-guide/cloud-storage/README.md) -- Storage provider details

---

*Last updated: March 7, 2026*
