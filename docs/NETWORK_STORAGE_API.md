<!--
SPDX-FileCopyrightText: 2024-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Network Storage API

Comprehensive reference for Yole's network storage subsystem. This API provides a unified, cross-platform interface for connecting to remote file storage services from within the Yole text editor.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Storage Protocols](#storage-protocols)
   - [FTP](#ftp)
   - [SFTP](#sftp)
   - [WebDAV](#webdav)
   - [Git](#git)
   - [SMB/CIFS](#smbcifs)
   - [Dropbox](#dropbox)
   - [Google Drive](#google-drive)
   - [OneDrive](#onedrive)
3. [Common Data Models](#common-data-models)
   - [NetworkDocument](#networkdocument)
   - [NetworkOperation](#networkoperation)
   - [SyncStatus](#syncstatus)
   - [DocumentSyncStatus](#documentsyncsync)
   - [NetworkStorage](#networkstorage)
   - [CacheEntry](#cacheentry)
   - [StorageQuota](#storagequota)
   - [DocumentPermission](#documentpermission)
4. [NetworkStorageService Interface](#networkstorageservice-interface)
5. [Database Layer](#database-layer)
6. [Authentication and Token Management](#authentication-and-token-management)
7. [Secure Storage API](#secure-storage-api)
8. [Error Handling](#error-handling)
9. [Code Examples](#code-examples)

---

## Architecture Overview

The network storage system is implemented as part of the `shared` Kotlin Multiplatform module and follows a layered architecture:

```
+-------------------------------------------------------------+
|                   NetworkStorageService                       |
|  (Unified interface for all storage protocol operations)     |
+-------------------------------------------------------------+
        |                    |                    |
+---------------+  +------------------+  +----------------+
| StorageConfig |  | AuthTokenManager |  | SecureStorage  |
| (per-protocol |  | (OAuth token     |  | (platform      |
|  config)      |  |  lifecycle)      |  |  keychain)     |
+---------------+  +------------------+  +----------------+
        |
+-------------------------------------------------------------+
|                  NetworkStorageDatabase                       |
|  (Persistent storage for documents, cache, operations, sync) |
+-------------------------------------------------------------+
```

**Package structure:**

```
digital.vasic.yole.network/
  NetworkStorageService.kt          -- Main service interface + StorageQuota
  common/
    StorageConfig.kt                -- Sealed class with per-protocol configs
    NetworkDocument.kt              -- Document/folder data model
    NetworkOperation.kt             -- Operation tracking data model
    SyncStatus.kt                   -- Sync status enum + DocumentSyncStatus
    NetworkStorage.kt               -- Storage instance metadata
    CacheEntry.kt                   -- Offline cache entry model
    DocumentPermission.kt           -- Permission enum
    NetworkStorageError.kt          -- Structured exception hierarchy
  database/
    NetworkStorageDatabase.kt       -- Database interface
  auth/
    AuthTokenManager.kt             -- OAuth/token manager
  platform/
    SecureStorage.kt                -- Platform-abstracted secure storage
```

All network operations are **suspend functions** or return **Kotlin Flows** for asynchronous, non-blocking execution. Every fallible operation returns a `Result<T>` wrapper.

---

## Storage Protocols

All protocols share a common base through the `StorageConfig` sealed class. Every config variant carries these shared properties:

| Property | Type | Description |
|---|---|---|
| `name` | `String` | Human-readable display name |
| `storageType` | `StorageType` | Enum identifier for the protocol |
| `isEnabled` | `Boolean` | Whether the storage is active |
| `priority` | `Int` | Display/operation priority (lower = higher priority) |
| `metadata` | `Map<String, String>` | Arbitrary key-value metadata |

Each config supports immutable update methods: `withEnabled()`, `withPriority()`, `withMetadata()`.

### StorageType Enum

```kotlin
enum class StorageType {
    WEBDAV, FTP, SFTP, SMB, GOOGLE_DRIVE, DROPBOX, ONEDRIVE, GIT
}
```

**Capabilities by type:**

| Storage Type | Display Name | Default Port | Supports Folders | Supports Encryption |
|---|---|---|---|---|
| `WEBDAV` | WebDAV | 443 | Yes | Yes |
| `FTP` | FTP | 21 | No | No |
| `SFTP` | SFTP | 22 | Yes | Yes |
| `SMB` | SMB/CIFS | 445 | Yes | Yes |
| `GOOGLE_DRIVE` | Google Drive | 443 | Yes | Yes |
| `DROPBOX` | Dropbox | 443 | Yes | Yes |
| `ONEDRIVE` | OneDrive | 443 | Yes | Yes |
| `GIT` | Git | 22 | Yes | Yes |

> **Note:** FTP is listed as not supporting reliable folder operations or encryption. Use SFTP for secure file transfer with folder support.

---

### FTP

**Config class:** `StorageConfig.FtpConfig`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `host` | `String` | -- | FTP server hostname |
| `port` | `Int` | `21` | Server port |
| `username` | `String` | -- | Login username |
| `password` | `String` | -- | Login password |
| `rootPath` | `String` | `"/"` | Base directory on the server |
| `passiveMode` | `Boolean` | `true` | Use passive mode for data connections |
| `secureFtp` | `Boolean` | `false` | Use FTPS (FTP over TLS) |
| `encoding` | `String` | `"UTF-8"` | Character encoding for file names |
| `connectionTimeout` | `Int` | `30000` | Connection timeout in milliseconds |

**Authentication:** Username/password only.

**Limitations:**
- No reliable folder support (`supportsFolders = false`)
- No encryption unless `secureFtp` is enabled (FTPS)
- Basic FTP transmits credentials in plaintext

**Example:**
```kotlin
val ftpConfig = StorageConfig.FtpConfig(
    name = "My FTP Server",
    host = "ftp.example.com",
    port = 21,
    username = "user",
    password = "pass",
    rootPath = "/documents",
    passiveMode = true
)
```

---

### SFTP

**Config class:** `StorageConfig.SftpConfig`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `host` | `String` | -- | SSH server hostname |
| `port` | `Int` | `22` | Server port |
| `username` | `String?` | `null` | Login username |
| `password` | `String?` | `null` | Login password (for password auth) |
| `privateKeyPath` | `String?` | `null` | Path to SSH private key file |
| `privateKeyPassphrase` | `String?` | `null` | Passphrase for the private key |
| `knownHostsPath` | `String?` | `null` | Path to known_hosts file |
| `strictHostKeyChecking` | `Boolean` | `true` | Verify server host key |
| `rootPath` | `String` | `"/"` | Base directory on the server |
| `useSsl` | `Boolean` | `true` | Use SSL/TLS |
| `connectionTimeout` | `Int` | `30000` | Connection timeout in milliseconds |

**Authentication methods:**
- Username/password
- SSH private key (with optional passphrase)
- Both can be combined

**Limitations:**
- Requires SSH access to the server
- Private key must be accessible on the local filesystem

**Example:**
```kotlin
val sftpConfig = StorageConfig.SftpConfig(
    name = "Dev Server",
    host = "ssh.example.com",
    port = 22,
    username = "deploy",
    privateKeyPath = "/home/user/.ssh/id_rsa",
    privateKeyPassphrase = null,
    strictHostKeyChecking = true,
    rootPath = "/var/www/notes"
)
```

---

### WebDAV

**Config class:** `StorageConfig.WebDavConfig`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `url` | `String` | -- | Full WebDAV endpoint URL |
| `username` | `String` | -- | Login username |
| `password` | `String` | -- | Login password or token |
| `authenticationType` | `WebDavAuthenticationType` | `BASIC` | Auth mechanism |
| `sslEnabled` | `Boolean` | `true` | Use HTTPS |
| `verifyCertificate` | `Boolean` | `true` | Verify SSL certificates |
| `connectionTimeout` | `Int` | `30000` | Connection timeout in milliseconds |
| `readTimeout` | `Int` | `60000` | Read timeout in milliseconds |

**Authentication types** (`WebDavAuthenticationType` enum):

| Value | Description |
|---|---|
| `BASIC` | HTTP Basic authentication |
| `DIGEST` | HTTP Digest authentication |
| `OAUTH` | OAuth 2.0 bearer token |
| `NONE` | No authentication |

**Supported operations:** All `NetworkStorageService` operations (list, upload, download, delete, create folder, rename, move, copy, sync, search).

**Example:**
```kotlin
val webdavConfig = StorageConfig.WebDavConfig(
    name = "Nextcloud",
    url = "https://cloud.example.com/remote.php/dav/files/user/",
    username = "user",
    password = "app-token-xyz",
    authenticationType = WebDavAuthenticationType.BASIC,
    sslEnabled = true,
    verifyCertificate = true
)
```

---

### Git

**Config class:** `StorageConfig.GitConfig`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `repositoryUrl` | `String` | -- | Git repository URL (HTTPS or SSH) |
| `branch` | `String` | `"main"` | Branch to work with |
| `username` | `String?` | `null` | HTTPS username |
| `password` | `String?` | `null` | HTTPS password |
| `personalAccessToken` | `String?` | `null` | Personal access token (PAT) |
| `privateKeyPath` | `String?` | `null` | Path to SSH private key |
| `privateKeyPassphrase` | `String?` | `null` | Passphrase for the private key |
| `localCachePath` | `String` | -- | Local directory for the cloned repository |
| `autoSync` | `Boolean` | `true` | Automatically sync (pull/push) on changes |
| `commitAuthorName` | `String` | `"Yole"` | Author name for commits |
| `commitAuthorEmail` | `String` | `"yole@example.com"` | Author email for commits |
| `connectionTimeout` | `Int` | `30000` | Connection timeout in milliseconds |

**Authentication methods:**
- HTTPS with username/password
- HTTPS with personal access token (PAT)
- SSH with private key (with optional passphrase)

**Limitations:**
- Requires a writable local cache directory (`localCachePath`)
- Binary files are stored but may result in large repository sizes
- Merge conflicts must be resolved manually

**Example:**
```kotlin
val gitConfig = StorageConfig.GitConfig(
    name = "Notes Repo",
    repositoryUrl = "https://github.com/user/notes.git",
    branch = "main",
    personalAccessToken = "ghp_xxxxxxxxxxxx",
    localCachePath = "/data/user/yole/git-cache/notes",
    autoSync = true,
    commitAuthorName = "Jane Doe",
    commitAuthorEmail = "jane@example.com"
)
```

---

### SMB/CIFS

**Config class:** `StorageConfig.SmbConfig`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `host` | `String` | -- | SMB server hostname or IP |
| `share` | `String` | -- | Network share name |
| `domain` | `String?` | `null` | Windows domain (for domain auth) |
| `username` | `String` | -- | Login username |
| `password` | `String` | -- | Login password |
| `path` | `String` | `"/"` | Initial path within the share |
| `port` | `Int` | `445` | Server port |
| `encryption` | `Boolean` | `true` | Use SMB encryption |
| `signing` | `Boolean` | `true` | Use SMB signing |
| `useSsl` | `Boolean` | `false` | Use SSL/TLS transport |
| `connectionTimeout` | `Int` | `30000` | Connection timeout in milliseconds |

**Authentication:** Username/password with optional Windows domain.

**Limitations:**
- SMB protocol is primarily designed for local networks
- Performance may be poor over high-latency connections
- SSL/TLS transport (`useSsl`) is disabled by default

**Example:**
```kotlin
val smbConfig = StorageConfig.SmbConfig(
    name = "Office NAS",
    host = "192.168.1.100",
    share = "documents",
    domain = "CORP",
    username = "user",
    password = "pass",
    path = "/notes",
    encryption = true,
    signing = true
)
```

---

### Dropbox

**Config class:** `StorageConfig.DropboxConfig`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `accessToken` | `String` | -- | OAuth 2.0 access token |
| `appKey` | `String` | -- | Dropbox app key |
| `appSecret` | `String` | -- | Dropbox app secret |
| `refreshToken` | `String?` | `null` | OAuth 2.0 refresh token |
| `rootPath` | `String` | `""` | Root path within Dropbox (empty = root) |

**Authentication:** OAuth 2.0 with app key/secret. Tokens are managed via `AuthTokenManager`.

**Limitations:**
- Requires Dropbox API app registration
- Rate limits apply per the Dropbox API terms
- `rootPath` defaults to empty string (Dropbox root), not `"/"`

**Example:**
```kotlin
val dropboxConfig = StorageConfig.DropboxConfig(
    name = "My Dropbox",
    accessToken = "sl.xxxxx",
    appKey = "abc123",
    appSecret = "secret456",
    refreshToken = "refresh_token_xxx",
    rootPath = "/Apps/Yole"
)
```

---

### Google Drive

**Config class:** `StorageConfig.GoogleDriveConfig`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `clientId` | `String` | -- | OAuth 2.0 client ID |
| `clientSecret` | `String` | -- | OAuth 2.0 client secret |
| `refreshToken` | `String?` | `null` | OAuth 2.0 refresh token |
| `accessToken` | `String?` | `null` | OAuth 2.0 access token |
| `rootFolderId` | `String?` | `null` | Root folder ID (null = My Drive root) |
| `teamDriveId` | `String?` | `null` | Shared/Team Drive ID |

**Authentication:** OAuth 2.0 with client credentials. Token refresh is handled by `AuthTokenManager`.

**Limitations:**
- Requires Google Cloud Console app registration
- Google Drive uses folder IDs rather than paths
- Team Drive access requires `teamDriveId`
- API quotas apply

**Example:**
```kotlin
val gdriveConfig = StorageConfig.GoogleDriveConfig(
    name = "Google Drive",
    clientId = "xxxx.apps.googleusercontent.com",
    clientSecret = "GOCSPX-xxxx",
    refreshToken = "1//0xxxx",
    rootFolderId = null // Use My Drive root
)
```

---

### OneDrive

**Config class:** `StorageConfig.OneDriveConfig`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `clientId` | `String` | -- | Azure AD application (client) ID |
| `clientSecret` | `String` | -- | Azure AD client secret |
| `refreshToken` | `String?` | `null` | OAuth 2.0 refresh token |
| `accessToken` | `String?` | `null` | OAuth 2.0 access token |
| `driveType` | `OneDriveDriveType` | `ME` | Type of OneDrive to access |
| `driveId` | `String?` | `null` | Specific drive ID (for non-ME types) |
| `rootFolderId` | `String?` | `null` | Root folder ID (null = drive root) |

**Drive types** (`OneDriveDriveType` enum):

| Value | Description |
|---|---|
| `ME` | Personal OneDrive |
| `BUSINESS` | Business OneDrive |
| `SHAREPOINT` | SharePoint document library |
| `GROUP` | Microsoft 365 group drive |

**Authentication:** OAuth 2.0 via Azure AD. Token refresh is handled by `AuthTokenManager`.

**Limitations:**
- Requires Azure AD app registration
- Different drive types may require different API permissions
- SharePoint and Group drives require explicit `driveId`

**Example:**
```kotlin
val onedriveConfig = StorageConfig.OneDriveConfig(
    name = "OneDrive",
    clientId = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    clientSecret = "client-secret",
    refreshToken = "M.R3_BAY...",
    driveType = OneDriveDriveType.ME
)
```

---

## Common Data Models

### NetworkDocument

**Package:** `digital.vasic.yole.network.common`

Represents a file or folder stored on a network storage location.

**Properties:**

| Property | Type | Default | Description |
|---|---|---|---|
| `id` | `String` | -- | Unique identifier within the storage |
| `name` | `String` | -- | Human-readable file/folder name |
| `path` | `String` | `""` | Full path within the storage |
| `isFolder` | `Boolean` | `false` | Whether this entry is a folder |
| `size` | `Long` | `0L` | File size in bytes (0 for folders) |
| `lastModified` | `Instant?` | `null` | Last modification timestamp |
| `syncStatus` | `SyncStatus` | `UNKNOWN` | Current synchronization status |
| `documentId` | `String?` | `null` | Associated local document ID |
| `contentType` | `String?` | `null` | MIME type (null for folders) |
| `extension` | `String` | derived | File extension without dot |
| `parentPath` | `String` | derived | Parent directory path |
| `isSyncing` | `Boolean` | derived | True if `syncStatus == SYNCING` |
| `hasPendingChanges` | `Boolean` | derived | True if `syncStatus == PENDING_UPLOAD` |
| `isAvailableOffline` | `Boolean` | derived | True if `syncStatus == SYNCED` |
| `isReadOnly` | `Boolean` | `false` | Whether the document is read-only |
| `isHidden` | `Boolean` | `false` | Whether the document is hidden |
| `metadata` | `Map<String, String>` | `emptyMap()` | Provider-specific metadata |
| `thumbnails` | `List<String>` | `emptyList()` | Thumbnail URLs or paths |
| `tags` | `List<String>` | `emptyList()` | Tags/labels |
| `owner` | `String?` | `null` | Document owner |
| `permissions` | `Set<DocumentPermission>` | `emptySet()` | Granted permissions |
| `storageId` | `String` | `""` | ID of the containing storage |
| `author` | `String?` | `null` | Document author |

**Computed properties:**

| Property | Type | Description |
|---|---|---|
| `isTextFile` | `Boolean` | True if the extension matches known text file types |
| `isImageFile` | `Boolean` | True if the extension matches known image types |
| `isPdfFile` | `Boolean` | True if extension is `"pdf"` |
| `isPreviewable` | `Boolean` | True if text, image, or PDF |
| `isEditable` | `Boolean` | True if text file and not read-only |
| `formattedSize` | `String` | Human-readable size (e.g., `"1KB"`, `"5MB"`) |

**Recognized text extensions:** `txt`, `md`, `markdown`, `rst`, `adoc`, `org`, `wiki`, `tex`, `json`, `xml`, `yaml`, `yml`, `toml`, `ini`, `cfg`, `conf`, `csv`, `tsv`, `log`, `sql`, `sh`, `bat`, `ps1`, `py`, `js`, `ts`, `kt`, `java`, `cpp`, `c`, `h`, `hpp`, `go`, `rs`, `php`, `rb`, `swift`, `dart`, `scala`, `clj`, `hs`, `ml`

**Recognized image extensions:** `jpg`, `jpeg`, `png`, `gif`, `bmp`, `webp`, `svg`, `ico`, `tiff`, `tif`, `psd`, `raw`, `cr2`, `nef`, `arw`

**Key methods:**

- `isInPath(path: String): Boolean` -- checks if this document is inside the given directory (including subdirectories)
- `isDirectChildOf(path: String): Boolean` -- checks if this document is a direct child of the given directory
- `withSyncStatus(syncStatus: SyncStatus): NetworkDocument` -- returns a copy with updated sync status
- `withDocumentId(documentId: String?): NetworkDocument` -- returns a copy with updated local document ID
- `withMetadata(metadata: Map<String, String>): NetworkDocument` -- returns a copy with updated metadata
- `withPermissions(permissions: Set<DocumentPermission>): NetworkDocument` -- returns a copy with updated permissions

**Factory methods:**

- `NetworkDocument.mock(...)` -- creates a mock file document for testing
- `NetworkDocument.mockFolder(...)` -- creates a mock folder document for testing

---

### NetworkOperation

**Package:** `digital.vasic.yole.network.common`

Tracks the state and progress of an asynchronous network operation.

**Properties:**

| Property | Type | Default | Description |
|---|---|---|---|
| `id` | `Long` | -- | Unique operation identifier |
| `type` | `Type` | -- | Operation type |
| `remotePath` | `String` | -- | Remote file/folder path |
| `localPath` | `String?` | `null` | Local file path (null for remote-only ops) |
| `status` | `Status` | `PENDING` | Current operation status |
| `progress` | `Double` | `0.0` | Progress from 0.0 to 1.0 |
| `totalSize` | `Long` | `0L` | Total bytes to transfer |
| `bytesTransferred` | `Long` | `0L` | Bytes transferred so far |
| `createdAt` | `Instant` | -- | When the operation was created |
| `startedAt` | `Instant?` | `null` | When the operation started |
| `completedAt` | `Instant?` | `null` | When the operation completed |
| `error` | `String?` | `null` | Error message if failed |
| `retryCount` | `Int` | `0` | Number of retry attempts made |
| `maxRetries` | `Int` | `3` | Maximum retry attempts allowed |
| `priority` | `Int` | `100` | Operation priority (higher = higher) |
| `canPause` | `Boolean` | `true` | Whether the operation supports pausing |
| `canCancel` | `Boolean` | `true` | Whether the operation can be cancelled |
| `isPaused` | `Boolean` | `false` | Whether the operation is currently paused |
| `estimatedTimeRemaining` | `Long?` | `null` | Estimated remaining time in milliseconds |
| `transferSpeed` | `Long?` | `null` | Transfer speed in bytes/second |
| `metadata` | `Map<String, String>` | `emptyMap()` | Additional metadata |

**Operation types** (`NetworkOperation.Type`):

| Value | Description |
|---|---|
| `UPLOAD` | Upload a local file to remote |
| `DOWNLOAD` | Download a remote file to local |
| `DELETE` | Delete a remote file/folder |
| `CREATE_FOLDER` | Create a new remote folder |
| `RENAME` | Rename a remote file/folder |
| `COPY` | Copy a remote file/folder |
| `MOVE` | Move a remote file/folder |
| `SYNC` | Synchronize files |

**Operation statuses** (`NetworkOperation.Status`):

| Value | Description |
|---|---|
| `PENDING` | Queued, not yet started |
| `IN_PROGRESS` | Currently executing |
| `PAUSED` | Paused by user |
| `COMPLETED` | Finished successfully |
| `FAILED` | Finished with error |
| `CANCELLED` | Cancelled by user |

**Computed properties:**

- `isRunning: Boolean` -- `status == IN_PROGRESS && !isPaused`
- `isCompleted: Boolean` -- `status == COMPLETED`
- `hasFailed: Boolean` -- `status == FAILED`
- `canRetry: Boolean` -- `hasFailed && retryCount < maxRetries`
- `isPending: Boolean` -- `status == PENDING`
- `progressPercentage: Int` -- progress as 0-100
- `duration: Long?` -- elapsed time in milliseconds

**Key methods:**

- `withStatus(status, error?)` -- returns a copy with new status (auto-sets `startedAt`/`completedAt`)
- `withProgress(progress, bytesTransferred?, transferSpeed?, estimatedTimeRemaining?)` -- returns a copy with updated progress (clamped 0.0..1.0)
- `withError(error)` -- returns a copy with `FAILED` status, increments `retryCount`
- `withPauseStatus(isPaused)` -- returns a copy toggling pause
- `withPriority(priority)` -- returns a copy with new priority

**Factory methods:**

- `NetworkOperation.createUpload(id, localPath, remotePath, totalSize?, priority?)` -- creates an upload operation
- `NetworkOperation.createDownload(id, remotePath, localPath, totalSize?, priority?)` -- creates a download operation
- `NetworkOperation.createDelete(id, remotePath, priority?)` -- creates a delete operation (`canPause = false`)
- `NetworkOperation.createFolder(id, remotePath, priority?)` -- creates a create-folder operation (`canPause = false`)
- `NetworkOperation.createSync(id, remotePath, priority?)` -- creates a sync operation
- `NetworkOperation.error(id, operationType, remotePath, localPath?, error)` -- creates an already-failed operation

---

### SyncStatus

**Package:** `digital.vasic.yole.network.common`

Enum representing the synchronization state of a document.

| Value | Description |
|---|---|
| `UNKNOWN` | Unknown or unrecognized status |
| `SYNCED` | Document is synchronized and up to date |
| `PENDING_UPLOAD` | Local changes need to be uploaded |
| `PENDING_DOWNLOAD` | Remote changes need to be downloaded |
| `SYNCING` | Currently being synchronized |
| `SYNC_ERROR` | Synchronization failed |
| `NOT_SYNCED` | Not synchronized (offline mode or disabled) |
| `QUEUED` | Queued for synchronization |
| `CONFLICT` | Conflicts exist that need resolution |
| `UPLOADING` | Document is being uploaded |
| `DOWNLOADING` | Document is being downloaded |

---

### DocumentSyncStatus

**Package:** `digital.vasic.yole.network.common`

Detailed synchronization status with metadata.

**Properties:**

| Property | Type | Default | Description |
|---|---|---|---|
| `status` | `SyncStatus` | -- | Current sync status |
| `lastSyncTime` | `Instant?` | `null` | Last successful sync timestamp |
| `nextSyncTime` | `Instant?` | `null` | Next scheduled sync timestamp |
| `errorMessage` | `String?` | `null` | Error message if sync failed |
| `retryCount` | `Int` | `0` | Retries attempted |
| `maxRetries` | `Int` | `3` | Maximum retries |
| `progress` | `Double` | `0.0` | Current sync progress (0.0 to 1.0) |
| `estimatedTimeRemaining` | `Long?` | `null` | Estimated time remaining |
| `dataSize` | `Long` | `0L` | Size of data being synced |
| `bytesTransferred` | `Long` | `0L` | Bytes transferred so far |
| `isAutomatic` | `Boolean` | `true` | Whether sync is automatic |
| `isAvailableOffline` | `Boolean` | `false` | Whether available offline |
| `hasConflicts` | `Boolean` | `false` | Whether conflicts exist |
| `conflictResolution` | `ConflictResolution?` | `null` | Chosen conflict strategy |
| `metadata` | `Map<String, String>` | `emptyMap()` | Additional metadata |

**Computed properties:**

- `isSyncing` -- true if status is `SYNCING`, `UPLOADING`, or `DOWNLOADING`
- `hasFailed` -- true if status is `SYNC_ERROR`
- `canRetry` -- true if failed and retries remain
- `isPending` -- true if status is `PENDING_UPLOAD`, `PENDING_DOWNLOAD`, or `QUEUED`
- `progressPercentage` -- progress as 0-100
- `transferSpeed` -- bytes/second (if computable)

**ConflictResolution enum:**

| Value | Description |
|---|---|
| `LOCAL_WINS` | Overwrite remote with local version |
| `REMOTE_WINS` | Overwrite local with remote version |
| `KEEP_BOTH` | Keep both versions (create a copy) |
| `MANUAL` | Manually resolve the conflict |
| `SKIP` | Skip synchronization until resolved |

---

### NetworkStorage

**Package:** `digital.vasic.yole.network.common`

Represents a configured network storage location with runtime metadata.

**Properties:**

| Property | Type | Default | Description |
|---|---|---|---|
| `id` | `String` | -- | Unique storage instance identifier |
| `name` | `String` | -- | Human-readable name |
| `type` | `StorageType` | -- | Storage protocol type |
| `location` | `String` | -- | Endpoint URL or path |
| `totalSpace` | `Long?` | `null` | Total space in bytes |
| `usedSpace` | `Long?` | `null` | Used space in bytes |
| `isOnline` | `Boolean` | `false` | Whether currently accessible |
| `lastSync` | `Instant?` | `null` | Last synchronization time |
| `metadata` | `Map<String, String>` | `emptyMap()` | Storage-specific metadata |
| `isEnabled` | `Boolean` | `true` | Whether enabled |
| `priority` | `Int` | `100` | Display priority |
| `supportsFolders` | `Boolean` | `true` | Whether folder operations are supported |
| `supportsMetadata` | `Boolean` | `true` | Whether metadata operations are supported |
| `maxFileSize` | `Long?` | `null` | Maximum file size in bytes |
| `supportedExtensions` | `List<String>` | `emptyList()` | Allowed extensions (empty = all) |

**Computed properties:**

- `availableSpace: Long?` -- `totalSpace - usedSpace` (null if either is null)
- `usagePercentage: Double?` -- usage as 0.0 to 1.0
- `isFull: Boolean` -- true if available space is 0
- `isLowOnSpace: Boolean` -- true if usage exceeds 90%

---

### CacheEntry

**Package:** `digital.vasic.yole.network.common`

Manages a local cached copy of a remote document for offline access.

**Properties:**

| Property | Type | Default | Description |
|---|---|---|---|
| `id` | `String` | -- | Unique cache entry identifier |
| `remoteDocumentId` | `String` | -- | Remote document this entry caches |
| `localPath` | `String` | -- | Local file path for cached content |
| `remotePath` | `String` | -- | Remote path of the original |
| `size` | `Long` | -- | Cached file size in bytes |
| `createdAt` | `Instant` | -- | When the cache entry was created |
| `lastAccessed` | `Instant` | -- | When last accessed |
| `lastModified` | `Instant` | -- | When last modified |
| `expiresAt` | `Instant?` | `null` | Expiration time (null = no expiration) |
| `isValid` | `Boolean` | derived | True if not expired |
| `isPinned` | `Boolean` | `false` | If true, will not be evicted |
| `isInUse` | `Boolean` | `false` | If true, currently being accessed |
| `accessCount` | `Int` | `0` | Number of times accessed |
| `contentType` | `String?` | `null` | MIME type |
| `checksum` | `String?` | `null` | Content hash for integrity |
| `compression` | `String?` | `null` | Compression algorithm used |
| `originalSize` | `Long?` | `null` | Size before compression |
| `priority` | `Int` | `100` | Eviction priority (higher = less likely) |
| `metadata` | `Map<String, String>` | `emptyMap()` | Additional metadata |
| `tags` | `List<String>` | `emptyList()` | Categorization tags |

**Computed properties:**

- `isExpired: Boolean` -- `!isValid`
- `canBeEvicted: Boolean` -- `!isPinned && !isInUse`
- `compressionRatio: Double?` -- compressed/original size ratio
- `age: Long` -- milliseconds since creation
- `timeSinceLastAccess: Long` -- milliseconds since last access
- `accessFrequency: Double` -- approximate accesses per day

**Factory method:**

```kotlin
CacheEntry.create(
    remoteDocumentId = "doc-123",
    localPath = "/cache/doc.txt",
    remotePath = "/documents/doc.txt",
    size = 2048L,
    contentType = "text/plain",
    ttl = 3600000L,     // 1 hour TTL in milliseconds
    isPinned = false
)
```

---

### StorageQuota

**Package:** `digital.vasic.yole.network`

Storage quota information returned by `NetworkStorageService.getQuotaInfo()`.

**Properties:**

| Property | Type | Default | Description |
|---|---|---|---|
| `totalSpace` | `Long` | -- | Total quota in bytes |
| `usedSpace` | `Long` | -- | Used space in bytes |
| `availableSpace` | `Long` | -- | Available space in bytes |
| `usagePercentage` | `Double` | -- | Usage as 0.0 to 1.0 |
| `isFull` | `Boolean` | -- | Whether storage is at capacity |
| `isLowOnSpace` | `Boolean` | -- | Whether less than 10% available |
| `expiresAt` | `Instant?` | `null` | Quota expiration time |
| `metadata` | `Map<String, String>` | `emptyMap()` | Additional quota metadata |

**Formatted display properties:** `formattedTotalSpace`, `formattedUsedSpace`, `formattedAvailableSpace`, `usagePercentageString`.

---

### DocumentPermission

**Package:** `digital.vasic.yole.network.common`

Enum of granular permissions for documents and folders.

| Permission | Category | Description |
|---|---|---|
| `READ` | Read | Can read document content |
| `WRITE` | Write | Can modify document content |
| `DELETE` | Admin | Can delete the document |
| `CREATE` | Write | Can create new documents in this folder |
| `RENAME` | Write | Can rename the document |
| `MOVE` | Write | Can move the document |
| `COPY` | Write | Can copy the document |
| `SHARE` | Admin | Can share with other users |
| `MANAGE_PERMISSIONS` | Admin | Can change permissions |
| `VIEW_METADATA` | Read | Can view document metadata |
| `MODIFY_METADATA` | Write | Can modify document metadata |
| `EXECUTE` | Admin | Can execute the document |
| `DOWNLOAD` | Read | Can download the document |
| `UPLOAD` | Write | Can upload to this folder |
| `SYNC` | Admin | Can synchronize the document |
| `ADMIN` | Admin | Full administrative access (implies all others) |

**Predefined permission sets:**

- `DEFAULT_FOLDER_PERMISSIONS` -- READ, WRITE, CREATE, RENAME, MOVE, COPY, DELETE, UPLOAD, DOWNLOAD, SYNC
- `DEFAULT_DOCUMENT_PERMISSIONS` -- READ, WRITE, RENAME, MOVE, COPY, DELETE, DOWNLOAD, SYNC
- `READ_ONLY_PERMISSIONS` -- READ, VIEW_METADATA, DOWNLOAD
- `ALL_PERMISSIONS` -- all values

**Utility methods:**

- `DocumentPermission.hasPermission(permissions, permission)` -- checks if a set includes a permission (ADMIN grants all)
- `DocumentPermission.hasReadPermissions(permissions)` -- checks for any read permission
- `DocumentPermission.hasWritePermissions(permissions)` -- checks for any write permission
- `DocumentPermission.hasAdministrativePermissions(permissions)` -- checks for any admin permission
- `DocumentPermission.getEffectivePermissions(permissions)` -- expands ADMIN to all permissions

---

## NetworkStorageService Interface

**Package:** `digital.vasic.yole.network`

The central interface all storage protocol implementations must satisfy. All methods are either `suspend` functions or return `Flow` for asynchronous streaming.

### Connection Management

```kotlin
val config: StorageConfig                       // The storage configuration
val isOnline: Boolean                           // Whether currently connected
val rootPath: String                            // Root path (default "/")

suspend fun connect(): Result<Unit>             // Connect to the storage
suspend fun disconnect(): Result<Unit>          // Disconnect
suspend fun testConnection(): Result<Boolean>   // Test connectivity
suspend fun getStorageInfo(): NetworkStorage     // Get storage metadata
suspend fun getQuotaInfo(): Result<StorageQuota> // Get quota information
```

### File Operations

```kotlin
fun listFiles(path: String = "/"): Flow<Result<List<NetworkDocument>>>
suspend fun downloadFile(remotePath: String, localPath: String): Flow<NetworkOperation>
suspend fun uploadFile(localPath: String, remotePath: String): Flow<NetworkOperation>
suspend fun deleteFile(remotePath: String): Result<Unit>
suspend fun createFolder(remotePath: String): Result<NetworkDocument>
suspend fun renameFile(remotePath: String, newName: String): Result<Unit>
suspend fun moveFile(sourcePath: String, destinationPath: String): Result<NetworkDocument>
suspend fun copyFile(sourcePath: String, destinationPath: String): Result<Unit>
suspend fun getFileInfo(remotePath: String): Result<NetworkDocument>
suspend fun exists(remotePath: String): Result<Boolean>
fun getParentPath(remotePath: String): String?
fun validatePath(remotePath: String): Result<Unit>
```

### Operation Management

```kotlin
fun getActiveOperations(): Flow<List<NetworkOperation>>
suspend fun cancelOperation(operationId: Long): Result<Unit>
suspend fun pauseOperation(operationId: Long): Result<Unit>
suspend fun resumeOperation(operationId: Long): Result<Unit>
```

### Cache Management

```kotlin
fun getCacheEntries(path: String? = null): Flow<List<CacheEntry>>
suspend fun addToCache(remotePath: String, priority: Int = 100): Result<Unit>
suspend fun removeFromCache(remotePath: String): Result<Unit>
suspend fun clearCache(): Result<Unit>
```

### Synchronization

```kotlin
fun getSyncStatus(path: String? = null): Flow<Map<String, SyncStatus>>
suspend fun syncFile(remotePath: String, forceSync: Boolean = false): Flow<NetworkOperation>
suspend fun syncAll(forceSync: Boolean = false): Flow<NetworkOperation>
```

### Search and History

```kotlin
fun searchFiles(
    query: String,
    path: String? = null,
    includeContent: Boolean = false
): Flow<Result<List<NetworkDocument>>>

fun getRecentChanges(
    since: kotlinx.datetime.Instant,
    path: String? = null
): Flow<List<NetworkDocument>>
```

---

## Database Layer

**Interface:** `NetworkStorageDatabase`
**Package:** `digital.vasic.yole.network.database`

Provides persistent local storage for network storage metadata, cached files, operations, and sync state. All methods return `Result<T>` for error handling.

### Lifecycle

```kotlin
suspend fun initialize(): Result<Unit>
suspend fun close(): Result<Unit>
```

### Storage CRUD

```kotlin
suspend fun insertStorage(storage: NetworkStorage): Result<Unit>
suspend fun updateStorage(storage: NetworkStorage): Result<Unit>
suspend fun getStorage(id: String): Result<NetworkStorage?>
suspend fun getAllStorage(): Result<List<NetworkStorage>>
suspend fun deleteStorage(id: String): Result<Unit>
```

### Document CRUD

```kotlin
suspend fun insertDocument(document: NetworkDocument): Result<Unit>
suspend fun updateDocument(document: NetworkDocument): Result<Unit>
suspend fun getDocument(id: String): Result<NetworkDocument?>
suspend fun getDocumentsByStorage(storageId: String): Result<List<NetworkDocument>>
suspend fun getDocumentsByPath(path: String): Result<List<NetworkDocument>>
suspend fun deleteDocument(id: String): Result<Unit>
fun observeDocumentsByStorage(storageId: String): Flow<List<NetworkDocument>>
```

### Cache CRUD

```kotlin
suspend fun insertCacheEntry(entry: CacheEntry): Result<Unit>
suspend fun updateCacheEntry(entry: CacheEntry): Result<Unit>
suspend fun getCacheEntry(id: String): Result<CacheEntry?>
suspend fun getCacheEntriesByDocument(documentId: String): Result<List<CacheEntry>>
suspend fun getAllCacheEntries(): Result<List<CacheEntry>>
suspend fun deleteCacheEntry(id: String): Result<Unit>
suspend fun deleteExpiredCacheEntries(): Result<Int>    // Returns number deleted
suspend fun getCacheUsage(): Result<Long>               // Returns total cache size in bytes
```

### Operation CRUD

```kotlin
suspend fun insertOperation(operation: NetworkOperation): Result<Unit>
suspend fun updateOperation(operation: NetworkOperation): Result<Unit>
suspend fun getOperation(id: Long): Result<NetworkOperation?>
suspend fun getActiveOperations(): Result<List<NetworkOperation>>
suspend fun deleteOperation(id: Long): Result<Unit>
suspend fun clearCompletedOperations(): Result<Int>     // Returns number cleared
```

### Sync Status

```kotlin
suspend fun updateSyncStatus(remotePath: String, status: SyncStatus): Result<Unit>
suspend fun getSyncStatus(remotePath: String): Result<SyncStatus?>
suspend fun getAllSyncStatus(): Result<Map<String, SyncStatus>>
suspend fun deleteSyncStatus(remotePath: String): Result<Unit>
```

### Maintenance

```kotlin
suspend fun clearAll(): Result<Unit>        // Delete all data
suspend fun vacuum(): Result<Unit>          // Compact the database
```

---

## Authentication and Token Management

**Class:** `AuthTokenManager`
**Package:** `digital.vasic.yole.network.auth`

Manages OAuth 2.0 and other authentication tokens for cloud storage services (Dropbox, Google Drive, OneDrive). Uses `SecureStorage` for persisting sensitive data and a `Mutex` for thread safety.

### Construction

```kotlin
val tokenManager = AuthTokenManager(
    serviceName = "google_drive",  // Prefix for all stored keys
    secureStorage = null           // Optional; auto-created via SecureStorageFactory if null
)
```

### Token Storage

```kotlin
// Store individual tokens
suspend fun storeAccessToken(token: String): Result<Unit>
suspend fun storeRefreshToken(token: String): Result<Unit>
suspend fun storeTokenExpiration(expiresAt: Instant): Result<Unit>

// Store all token information at once
suspend fun storeTokenInfo(
    accessToken: String,
    refreshToken: String? = null,
    expiresIn: Long? = null          // Seconds until expiration
): Result<Unit>
```

### Token Retrieval

```kotlin
suspend fun getAccessToken(): Result<String?>
suspend fun getRefreshToken(): Result<String?>
```

### Token Validation

```kotlin
suspend fun isTokenExpired(): Result<Boolean>    // True if expired or no expiration info
suspend fun hasValidToken(): Result<Boolean>     // True if access token exists and not expired
```

### Token Cleanup

```kotlin
suspend fun clearTokens(): Result<Unit>          // Remove all tokens for this service
```

### Debugging

```kotlin
suspend fun getTokenInfo(): Result<TokenInfo>
```

Returns a `TokenInfo` data class (no sensitive data):

```kotlin
data class TokenInfo(
    val hasAccessToken: Boolean,
    val hasRefreshToken: Boolean,
    val isExpired: Boolean,
    val serviceName: String,
    val timestamp: Instant
)
```

### Storage Key Scheme

The `AuthTokenManager` stores data under these keys (where `{serviceName}` is the constructor parameter):

| Key Pattern | Content |
|---|---|
| `{serviceName}_access_token` | Access token |
| `{serviceName}_refresh_token` | Refresh token |
| `{serviceName}_expires` | Expiration epoch milliseconds as string |

### Thread Safety

All public methods acquire a `Mutex` lock before accessing secure storage. Internal methods (suffixed `Internal`) do not acquire the lock and are only called from within a `mutex.withLock` block to prevent deadlocks from nested lock acquisition.

---

## Secure Storage API

**Interface:** `SecureStorage`
**Package:** `digital.vasic.yole.network.platform`

Platform-abstracted interface for securely storing sensitive data (passwords, tokens, keys). Platform implementations use the native keychain/keystore:

- **Android:** Android Keystore / EncryptedSharedPreferences
- **iOS:** iOS Keychain Services
- **Desktop:** OS-specific credential managers
- **Web:** Browser secure storage APIs

### Core Operations

```kotlin
suspend fun store(key: String, value: String): Result<Unit>
suspend fun retrieve(key: String): Result<String?>
suspend fun delete(key: String): Result<Unit>
suspend fun contains(key: String): Result<Boolean>
suspend fun listKeys(): Result<List<String>>
suspend fun clear(): Result<Unit>
suspend fun isSecure(): Result<Boolean>
```

### Credential Operations

High-level methods that store username/password pairs as `"username:password"` under the key `"{service}_credentials"`.

```kotlin
suspend fun storeCredentials(service: String, username: String, password: String): Result<Unit>
suspend fun retrieveCredentials(service: String): Result<Pair<String, String>?>
suspend fun deleteCredentials(service: String): Result<Unit>
```

### Token Operations

Store/retrieve authentication tokens under the key `"{service}_token"`.

```kotlin
suspend fun storeToken(service: String, token: String): Result<Unit>
suspend fun retrieveToken(service: String): Result<String?>
suspend fun deleteToken(service: String): Result<Unit>
```

### Private Key Operations

Store/retrieve SSH or other private keys under the key `"{service}_private_key"`.

```kotlin
suspend fun storePrivateKey(service: String, privateKey: String): Result<Unit>
suspend fun retrievePrivateKey(service: String): Result<String?>
suspend fun deletePrivateKey(service: String): Result<Unit>
```

### Factory

```kotlin
expect object SecureStorageFactory {
    suspend fun create(): Result<SecureStorage>
    suspend fun isAvailable(): Boolean
}
```

`SecureStorageFactory` is an `expect` declaration -- each platform provides its own `actual` implementation. Call `isAvailable()` to check if the current platform supports secure storage before use.

---

## Error Handling

**Sealed class:** `NetworkStorageException`
**Package:** `digital.vasic.yole.network.common`

All network storage errors are structured as a sealed exception hierarchy. Every exception carries an `errorCode` string and a `timestamp`.

### Exception Hierarchy

```
NetworkStorageException
  +-- ConnectionException
  |     +-- Failed               (CONNECTION_FAILED)
  |     +-- Timeout              (CONNECTION_TIMEOUT)
  |     +-- Authentication       (AUTHENTICATION_FAILED)
  |     +-- SslError             (SSL_ERROR)
  |     +-- ServerUnreachable    (SERVER_UNREACHABLE)
  |     +-- NetworkUnavailable   (NETWORK_UNAVAILABLE)
  |     +-- NotConnected         (NOT_CONNECTED)
  +-- FileOperationException
  |     +-- NotFound             (FILE_NOT_FOUND)
  |     +-- ListFailed           (LIST_FAILED)
  |     +-- UploadFailed         (UPLOAD_FAILED)
  |     +-- DownloadFailed       (DOWNLOAD_FAILED)
  |     +-- CreateFolderFailed   (CREATE_FOLDER_FAILED)
  |     +-- CopyFailed           (COPY_FAILED)
  |     +-- MoveFailed           (MOVE_FAILED)
  |     +-- DeleteFailed         (DELETE_FAILED)
  |     +-- PermissionDenied     (PERMISSION_DENIED)
  |     +-- AlreadyExists        (FILE_ALREADY_EXISTS)
  |     +-- InsufficientSpace    (INSUFFICIENT_SPACE)
  |     +-- Locked               (FILE_LOCKED)
  |     +-- InfoFailed           (INFO_FAILED)
  +-- ProtocolException
  |     +-- Unsupported          (UNSUPPORTED_PROTOCOL)
  |     +-- VersionMismatch      (VERSION_MISMATCH)
  |     +-- ConfigurationError   (CONFIGURATION_ERROR)
  +-- SyncException
  |     +-- Conflict             (SYNC_CONFLICT)
  |     +-- Interrupted          (SYNC_INTERRUPTED)
  |     +-- RetryLimitExceeded   (RETRY_LIMIT_EXCEEDED)
  +-- QuotaException
  |     +-- Exceeded             (QUOTA_EXCEEDED)
  |     +-- BandwidthExceeded    (BANDWIDTH_QUOTA_EXCEEDED)
  +-- CacheException
  |     +-- Corruption           (CACHE_CORRUPTION)
  |     +-- EntryNotFound        (CACHE_ENTRY_NOT_FOUND)
  +-- GenericError               (GENERIC_ERROR)
```

### Conflict Types

Used by `SyncException.Conflict`:

| Value | Description |
|---|---|
| `BOTH_MODIFIED` | Both sides modified |
| `REMOTE_MODIFIED` | Remote modified while local had changes |
| `LOCAL_MODIFIED` | Local modified while remote had changes |
| `DELETE_MODIFY_CONFLICT` | Deleted on one side, modified on the other |
| `RENAME_CONFLICT` | Renamed differently on both sides |
| `UNKNOWN` | Unknown conflict type |

### Error Utility Methods

Every `NetworkStorageException` provides:

- `toUserMessage(): String` -- returns a user-friendly error description
- `isRetryable(): Boolean` -- true for `Timeout`, `NetworkUnavailable`, `ServerUnreachable`, `BandwidthExceeded`
- `isPermanentFailure(): Boolean` -- true for `Authentication`, `PermissionDenied`, `Unsupported`, `VersionMismatch`, `Exceeded`
- `getSuggestedAction(): String?` -- actionable advice for the user

### Creating Exceptions from Throwables

```kotlin
val exception = NetworkStorageException.fromThrowable(
    throwable = originalException,
    operation = "upload",
    filePath = "/local/file.txt",
    remotePath = "/remote/file.txt"
)
```

This method inspects the exception message for keywords (`timeout`, `authentication`, `permission`, `quota`) and maps to the appropriate subtype.

---

## Code Examples

### Connecting to WebDAV and Listing Files

```kotlin
val config = StorageConfig.WebDavConfig(
    name = "Nextcloud",
    url = "https://cloud.example.com/remote.php/dav/files/user/",
    username = "user",
    password = "app-password"
)

// Assume `service` implements NetworkStorageService for WebDAV
service.connect().onSuccess {
    service.listFiles("/Documents").collect { result ->
        result.onSuccess { documents ->
            documents.forEach { doc ->
                println("${doc.name} (${doc.formattedSize}) - ${doc.syncStatus}")
            }
        }.onFailure { error ->
            println("Error: ${error.message}")
        }
    }
}
```

### Uploading a File with Progress Tracking

```kotlin
val uploadFlow = service.uploadFile(
    localPath = "/data/user/notes/meeting.md",
    remotePath = "/Documents/meeting.md"
)

uploadFlow.collect { operation ->
    when (operation.status) {
        NetworkOperation.Status.IN_PROGRESS -> {
            println("Upload: ${operation.progressPercentage}% " +
                    "(${operation.bytesTransferred}/${operation.totalSize} bytes)")
        }
        NetworkOperation.Status.COMPLETED -> {
            println("Upload complete in ${operation.duration}ms")
        }
        NetworkOperation.Status.FAILED -> {
            println("Upload failed: ${operation.error}")
            if (operation.canRetry) {
                println("Retry ${operation.retryCount}/${operation.maxRetries}")
            }
        }
        else -> {}
    }
}
```

### Managing OAuth Tokens for Cloud Storage

```kotlin
val tokenManager = AuthTokenManager(serviceName = "dropbox")

// Store tokens after OAuth flow
tokenManager.storeTokenInfo(
    accessToken = "sl.xxxxx",
    refreshToken = "refresh_xxx",
    expiresIn = 14400  // 4 hours in seconds
)

// Later, check token validity before making API calls
val isValid = tokenManager.hasValidToken().getOrDefault(false)
if (!isValid) {
    val refreshToken = tokenManager.getRefreshToken().getOrNull()
    if (refreshToken != null) {
        // Perform OAuth refresh flow, then store new tokens
        tokenManager.storeTokenInfo(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresIn = newExpiresIn
        )
    }
}

// Debug token state
val info = tokenManager.getTokenInfo().getOrNull()
println("Token info: has access=${info?.hasAccessToken}, " +
        "has refresh=${info?.hasRefreshToken}, expired=${info?.isExpired}")
```

### Storing Credentials Securely

```kotlin
val storage = SecureStorageFactory.create().getOrThrow()

// Store credentials for SFTP
storage.storeCredentials(
    service = "sftp_dev_server",
    username = "deploy",
    password = "secret"
)

// Store an SSH private key
storage.storePrivateKey(
    service = "sftp_dev_server",
    privateKey = "-----BEGIN RSA PRIVATE KEY-----\n..."
)

// Later, retrieve them
val credentials = storage.retrieveCredentials("sftp_dev_server").getOrNull()
credentials?.let { (username, password) ->
    println("Username: $username")
}

val key = storage.retrievePrivateKey("sftp_dev_server").getOrNull()
```

### Caching Files for Offline Access

```kotlin
// Add a file to the offline cache with high priority
service.addToCache(remotePath = "/Documents/important.md", priority = 200)

// List cached files
service.getCacheEntries().collect { entries ->
    entries.forEach { entry ->
        println("Cached: ${entry.remotePath} (${entry.size} bytes, " +
                "accessed ${entry.accessCount} times, " +
                "pinned=${entry.isPinned})")
    }
}

// Remove a file from cache
service.removeFromCache(remotePath = "/Documents/old-draft.md")

// Clear the entire cache
service.clearCache()
```

### Synchronization with Conflict Resolution

```kotlin
// Monitor sync status for all files
service.getSyncStatus().collect { statusMap ->
    statusMap.forEach { (path, status) ->
        when (status) {
            SyncStatus.CONFLICT -> println("CONFLICT: $path -- needs resolution")
            SyncStatus.SYNC_ERROR -> println("ERROR: $path")
            SyncStatus.PENDING_UPLOAD -> println("PENDING UPLOAD: $path")
            SyncStatus.SYNCED -> { /* up to date */ }
            else -> {}
        }
    }
}

// Force-sync a specific file
service.syncFile(remotePath = "/Documents/report.md", forceSync = true)
    .collect { operation ->
        if (operation.isCompleted) println("Sync complete")
    }

// Sync everything
service.syncAll(forceSync = false).collect { operation ->
    println("Syncing ${operation.remotePath}: ${operation.progressPercentage}%")
}
```

### Error Handling Pattern

```kotlin
val result = service.deleteFile("/Documents/old.md")

result.onSuccess {
    println("File deleted successfully")
}.onFailure { error ->
    when (error) {
        is NetworkStorageException.FileOperationException.NotFound ->
            println("File does not exist: ${error.filePath}")
        is NetworkStorageException.FileOperationException.PermissionDenied ->
            println("Access denied: ${error.requiredPermission}")
        is NetworkStorageException.ConnectionException.NetworkUnavailable ->
            println("No network -- queuing for later")
        is NetworkStorageException -> {
            println(error.toUserMessage())
            if (error.isRetryable()) {
                println("Suggested: ${error.getSuggestedAction()}")
            }
        }
        else -> println("Unexpected error: ${error.message}")
    }
}
```

### Database Layer Usage

```kotlin
// Initialize
val db: NetworkStorageDatabase = // platform-specific implementation
db.initialize()

// Store and query documents
db.insertDocument(document)
val docs = db.getDocumentsByStorage("storage-id").getOrDefault(emptyList())

// Observe document changes reactively
db.observeDocumentsByStorage("storage-id").collect { documents ->
    // UI update
}

// Cache maintenance
val deletedCount = db.deleteExpiredCacheEntries().getOrDefault(0)
val cacheSize = db.getCacheUsage().getOrDefault(0L)
println("Cache: ${cacheSize} bytes, cleaned $deletedCount expired entries")

// Operation tracking
val activeOps = db.getActiveOperations().getOrDefault(emptyList())
val clearedCount = db.clearCompletedOperations().getOrDefault(0)

// Maintenance
db.vacuum()
db.close()
```

### Checking Storage Quota

```kotlin
val quota = service.getQuotaInfo().getOrNull()
quota?.let {
    println("Storage: ${it.formattedUsedSpace} / ${it.formattedTotalSpace} " +
            "(${it.usagePercentageString})")
    if (it.isLowOnSpace) {
        println("Warning: storage is running low")
    }
    if (it.isFull) {
        println("Error: storage is full")
    }
}
```

---

## Source Files Reference

| File | Path |
|---|---|
| NetworkStorageService | `shared/src/commonMain/kotlin/digital/vasic/yole/network/NetworkStorageService.kt` |
| StorageConfig | `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/StorageConfig.kt` |
| NetworkDocument | `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/NetworkDocument.kt` |
| NetworkOperation | `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/NetworkOperation.kt` |
| SyncStatus | `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/SyncStatus.kt` |
| NetworkStorage | `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/NetworkStorage.kt` |
| CacheEntry | `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/CacheEntry.kt` |
| DocumentPermission | `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/DocumentPermission.kt` |
| NetworkStorageError | `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/NetworkStorageError.kt` |
| NetworkStorageDatabase | `shared/src/commonMain/kotlin/digital/vasic/yole/network/database/NetworkStorageDatabase.kt` |
| AuthTokenManager | `shared/src/commonMain/kotlin/digital/vasic/yole/network/auth/AuthTokenManager.kt` |
| SecureStorage | `shared/src/commonMain/kotlin/digital/vasic/yole/network/platform/SecureStorage.kt` |
