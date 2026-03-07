# Module 13: Cloud Storage Integration (7 videos)

## Video 13.1: Cloud Storage Protocol Overview (15 min)

### Timestamps
- 0:00 Introduction to Yole's cloud storage architecture
- 1:30 The `NetworkStorageService` interface: a unified API for 8 protocols
- 3:00 Overview of supported protocols: Dropbox, Google Drive, OneDrive, WebDAV, FTP, SFTP, Git, S3
- 5:00 Protocol categories: cloud APIs (OAuth2), self-hosted (WebDAV, FTP/SFTP), version control (Git)
- 7:00 Offline-first philosophy: local edits first, sync when connected
- 9:00 Architecture walkthrough: `StorageConfig`, `NetworkDocument`, `SyncStatus`
- 11:00 How protocols register and are selected by the user
- 13:00 Resilience patterns: CircuitBreaker, ConnectionLimiter, and RateLimitedStorageService applied to all protocols
- 14:00 Live demo: connecting to Dropbox and Google Drive side by side
- 14:30 Summary and module roadmap

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/NetworkStorageService.kt` -- Unified interface with `connect()`, `disconnect()`, `listFiles()`, `downloadFile()`, `uploadFile()`
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/StorageConfig.kt` -- Configuration model for all protocols
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/NetworkDocument.kt` -- Document metadata model returned by all services
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/CircuitBreaker.kt` -- Circuit breaker for failure resilience
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/ConnectionLimiter.kt` -- Semaphore-based connection limiting
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/RateLimitedStorageService.kt` -- Rate-limiting decorator for any service

### Key Code Walkthrough

The `NetworkStorageService` interface provides a consistent API across all 8 protocols:

```kotlin
interface NetworkStorageService {
    val config: StorageConfig
    suspend fun connect(): Result<Unit>
    suspend fun disconnect(): Result<Unit>
    val isOnline: Boolean
    fun listFiles(path: String = "/"): Flow<Result<List<NetworkDocument>>>
    suspend fun downloadFile(remotePath: String, localPath: String): Flow<Result<NetworkOperation>>
    suspend fun uploadFile(localPath: String, remotePath: String): Flow<Result<NetworkOperation>>
    suspend fun deleteFile(remotePath: String): Result<Unit>
    suspend fun searchFiles(query: String): Flow<Result<List<NetworkDocument>>>
}
```

### Exercises
1. **Compare protocol implementations** -- Open `DropboxService.kt`, `GoogleDriveService.kt`, and `WebDavService.kt` side by side. Identify how each implements `connect()` differently while exposing the same interface.
2. **Trace a file download** -- Set breakpoints in `NetworkStorageService.downloadFile()` and follow the call through a concrete implementation to see how Ktor HTTP requests are constructed.

---

## Video 13.2: Setting Up Dropbox Integration (18 min)

### Timestamps
- 0:00 Registering a Dropbox app on the developer portal
- 2:00 Obtaining client ID and client secret
- 4:00 Configuring `DropboxOAuth2Flow` with PKCE
- 6:00 The authorization URL: scopes and redirect URIs
- 8:00 Exchanging the authorization code for tokens
- 10:00 The `DropboxService` class: connecting and validating tokens
- 12:00 Listing files with `2/files/list_folder` API
- 14:00 Downloading and uploading via `content.dropboxapi.com`
- 15:30 File operations: rename, move, copy, delete
- 16:30 Search with `2/files/search_v2`
- 17:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/dropbox/DropboxService.kt` -- Full Dropbox API v2 integration
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/auth/OAuth2Flow.kt` -- `DropboxOAuth2Flow` typealias to Auth-KMP module
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/dropbox/DropboxServiceTest.kt` -- Service tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/dropbox/DropboxMockHttpTest.kt` -- Mock HTTP tests

### Exercises
1. **Complete the OAuth2 flow** -- Follow the Dropbox developer docs to register an app, then trace the `DropboxOAuth2Flow` code to understand how PKCE parameters are generated.
2. **Mock a file listing** -- Using `DropboxMockHttpTest.kt` as a reference, write a test that mocks a `list_folder` response with 5 files and verifies the `NetworkDocument` list.

---

## Video 13.3: Setting Up Google Drive Integration (18 min)

### Timestamps
- 0:00 Creating a Google Cloud project and enabling the Drive API
- 2:00 Configuring OAuth 2.0 credentials in Google Cloud Console
- 4:00 Scopes: `drive.file` vs. `drive.readonly` vs. full access
- 6:00 The `GoogleDriveOAuth2Flow` configuration
- 8:00 `GoogleDriveService.connect()`: token validation and account info
- 10:00 Listing files with query parameters and MIME type filtering
- 12:00 Downloading file content: binary vs. Google Docs export
- 14:00 Uploading with multipart upload API
- 15:30 Rate limiting and quota management with `AdaptiveRateLimiter`
- 17:00 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/googledrive/GoogleDriveService.kt` -- Google Drive REST API integration
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/googledrive/GoogleDriveMockHttpTest.kt` -- Mock HTTP tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/googledrive/GoogleDriveServiceTest.kt` -- Service tests

### Exercises
1. **Configure Drive scopes** -- Experiment with different OAuth2 scopes and observe how the `GoogleDriveService` behavior changes when given `drive.file` vs. `drive.readonly`.
2. **Test MIME type handling** -- Write a test that verifies how Google Docs files are exported to Markdown when downloaded through `GoogleDriveService`.

---

## Video 13.4: Setting Up OneDrive Integration (15 min)

### Timestamps
- 0:00 Registering an app in Azure AD (Microsoft Entra ID)
- 2:00 Microsoft Graph API vs. OneDrive-specific API
- 4:00 Configuring `OneDriveOAuth2Flow` with Azure AD endpoints
- 6:00 `OneDriveService.connect()`: Graph API token validation
- 8:00 File operations through Microsoft Graph endpoints
- 10:00 Delta queries for efficient synchronization
- 12:00 SharePoint document library access
- 13:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/onedrive/OneDriveService.kt` -- Microsoft Graph API integration
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/onedrive/OneDriveMockHttpTest.kt` -- Mock HTTP tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/onedrive/OneDriveServiceTest.kt` -- Service tests

### Exercises
1. **Trace a Graph API call** -- Follow a `listFiles()` call through `OneDriveService` and identify the exact Graph API endpoint, headers, and response parsing.
2. **Compare OAuth2 flows** -- Compare `OneDriveOAuth2Flow` with `DropboxOAuth2Flow` and `GoogleDriveOAuth2Flow`. Identify differences in authorization URLs, token endpoints, and scope formats.

---

## Video 13.5: WebDAV for Self-Hosted Storage (15 min)

### Timestamps
- 0:00 What is WebDAV and why it matters for self-hosted setups
- 1:30 Nextcloud and ownCloud WebDAV endpoints
- 3:00 `WebDavService` implementation: HTTP methods PROPFIND, PROPPATCH, MKCOL
- 5:00 Connecting with username and password (Basic/Digest auth)
- 7:00 Listing files: XML parsing of PROPFIND responses
- 9:00 File upload and download via PUT and GET
- 10:30 Parent directory filtering: handling WebDAV response quirks
- 12:00 Lock and unlock for concurrent editing scenarios
- 13:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/webdav/WebDavService.kt` -- WebDAV protocol implementation
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/webdav/WebDavMockHttpTest.kt` -- Mock HTTP tests with XML response mocking
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/webdav/WebDavServiceTest.kt` -- Service tests

### Key Code Walkthrough

WebDAV uses HTTP extensions for file management. The `WebDavService` builds XML requests:

```kotlin
// PROPFIND request to list files in a directory
val propfindXml = """
    <?xml version="1.0" encoding="UTF-8"?>
    <d:propfind xmlns:d="DAV:">
        <d:prop>
            <d:displayname/>
            <d:getcontentlength/>
            <d:getlastmodified/>
            <d:resourcetype/>
        </d:prop>
    </d:propfind>
""".trimIndent()
```

### Exercises
1. **Set up Nextcloud** -- Deploy a Nextcloud instance using Docker and configure `WebDavService` to connect to its WebDAV endpoint at `/remote.php/dav/files/USERNAME/`.
2. **Test XML parsing** -- Write a mock test that provides a PROPFIND XML response with mixed files and folders, and verify the parent directory is correctly filtered out.

---

## Video 13.6: FTP/SFTP and Git Storage (15 min)

### Timestamps
- 0:00 FTP protocol: when and why to use it
- 2:00 `FtpService`: connecting, listing, uploading, downloading
- 4:00 Active vs. passive mode and TLS (FTPS)
- 6:00 SFTP: secure file transfer over SSH
- 7:30 SSH key-based authentication with platform-specific key storage
- 9:00 Git as a storage backend: `GitService` implementation
- 10:30 Clone, pull, commit, push: the Git sync workflow
- 12:00 Merge conflict detection and user-facing resolution
- 13:30 Choosing the right protocol for your use case
- 14:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/ftp/FtpService.kt` -- FTP protocol implementation
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/git/GitService.kt` -- Git storage backend
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/git/GitMockHttpTest.kt` -- Git mock HTTP tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/ftp/FtpServiceTest.kt` -- FTP service tests

### Exercises
1. **Protocol comparison** -- Create a table comparing FTP, SFTP, and Git across dimensions: authentication, encryption, versioning, conflict resolution, and offline support.
2. **Git sync workflow** -- Trace a complete sync cycle through `GitService`: connect, pull changes, detect local edits, commit, and push.

---

## Video 13.7: OAuth2 Authentication and Sync Operations (20 min)

### Timestamps
- 0:00 OAuth2 fundamentals recap: authorization code grant with PKCE
- 2:00 The `OAuth2Flow` class: building the authorization URL
- 4:00 Token exchange: authorization code for access token + refresh token
- 6:00 Token storage with `SecureStorage` (platform keychain/keystore)
- 8:00 Automatic token refresh: detecting expiration, refreshing transparently
- 10:00 The `AuthTokenManager` interface and lifecycle
- 12:00 Sync operations: `SyncStatus` tracking across all protocols
- 14:00 Conflict detection: comparing remote vs. local file hashes
- 16:00 Conflict resolution: last-write-wins, three-way merge, user prompt
- 18:00 CancellationException handling and coroutine flow transparency
- 19:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/auth/OAuth2Flow.kt` -- Facade bridging to Auth-KMP module
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/auth/AuthTokenManager.kt` -- Token lifecycle management
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/SyncStatus.kt` -- Sync status tracking
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/platform/SecureStorage.kt` -- Platform `expect` for secure credential storage
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/auth/OAuth2FlowTest.kt` -- OAuth2 flow tests

### Key Code Walkthrough

The OAuth2 flow is a three-step process used by Dropbox, Google Drive, and OneDrive:

```kotlin
// Step 1: Generate authorization URL (opens browser)
val authUrl = oauth2Flow.getAuthorizationUrl(state = "random-state-value")

// Step 2: Exchange code for tokens (after user authorizes)
val tokenResult = oauth2Flow.exchangeCodeForToken(authorizationCode)

// Step 3: Refresh expired tokens (automatic)
val refreshResult = oauth2Flow.refreshAccessToken(refreshToken)
```

All service implementations rethrow `CancellationException` to maintain coroutine flow transparency:

```kotlin
try {
    val response = httpClient.get(endpoint)
    // process response
} catch (e: CancellationException) {
    throw e  // Never swallow cancellation
} catch (e: Exception) {
    // Handle other errors
}
```

### Exercises
1. **Token refresh simulation** -- Write a test that creates a `TokenResponse` with a past expiration time and verify the refresh flow triggers correctly through `AuthTokenManager`.
2. **Sync conflict scenario** -- Design a test where two clients modify the same file on different cloud services and trace how `SyncStatus` tracks the conflict state.
3. **Security audit** -- Verify that no token or credential is ever logged in plaintext by searching all service implementations for logging calls.
