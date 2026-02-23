# Module 7: Network Storage Integration (12 videos)

## Video 7.1: OAuth Authentication (18 min)

### Timestamps
- 0:00 Introduction to cloud storage authentication in Yole
- 1:30 OAuth 2.0 fundamentals: authorization code grant
- 3:00 PKCE (Proof Key for Code Exchange) for mobile/desktop apps
- 5:00 The `OAuth2Flow` class: building the authorization URL
- 7:00 Exchanging authorization codes for access tokens
- 9:00 Token storage with platform keychain/keystore via `SecureStorage`
- 11:00 Token refresh flow: detecting expiration and refreshing automatically
- 13:00 The `AuthTokenManager` interface and implementation
- 15:00 Error handling: revoked tokens, network failures, invalid grants
- 16:30 Security considerations: never storing tokens in plain text
- 17:30 Summary and next steps

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/auth/OAuth2Flow.kt` -- Core OAuth2 flow with authorization URL generation, token exchange, and refresh
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/auth/AuthTokenManager.kt` -- Token lifecycle management interface
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/platform/SecureStorage.kt` -- Platform `expect` for secure credential storage
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/auth/OAuth2FlowTest.kt` -- OAuth2 flow tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/auth/AuthTokenManagerTest.kt` -- Token manager tests

### Key Code Walkthrough

The `OAuth2Flow` class handles the entire token lifecycle:

```kotlin
// 1. Generate authorization URL
val authUrl = oauth2Flow.getAuthorizationUrl(state = "random-state-value")
// Opens browser to: https://provider.com/oauth2/authorize?client_id=...&response_type=code&...

// 2. Exchange code for token (after user authorizes)
val tokenResult = oauth2Flow.exchangeCodeForToken(authorizationCode)

// 3. Refresh expired token
val refreshResult = oauth2Flow.refreshAccessToken(refreshToken)
```

### Exercises
1. **Configure Dropbox connection** -- Walk through the OAuth2 flow for Dropbox: register an app on the Dropbox developer portal, obtain client ID/secret, and trace the flow through `OAuth2Flow`.
2. **Inspect token storage** -- Set breakpoints in `AuthTokenManager` and observe how tokens are stored and retrieved from `SecureStorage`.
3. **Simulate token expiration** -- Write a test that creates a token with a past expiration time and verify the refresh flow triggers correctly.

---

## Video 7.2: Dropbox API Integration (15 min)

### Timestamps
- 0:00 Dropbox API v2 overview
- 1:30 The `DropboxService` class: implementing `NetworkStorageService`
- 3:00 Connection flow: `connect()` validates token and fetches account info
- 4:30 File listing: `listFiles()` calls `2/files/list_folder`
- 6:00 File download: `downloadFile()` via `content.dropboxapi.com`
- 7:30 File upload: `uploadFile()` via `content.dropboxapi.com`
- 9:00 File operations: delete, rename, move, copy
- 10:30 Search: `searchFiles()` calls `2/files/search_v2`
- 11:30 Conflict resolution: handling concurrent modifications
- 12:30 Cache and sync status tracking with in-memory maps
- 13:30 Current limitations and planned improvements
- 14:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/dropbox/DropboxService.kt` -- Full Dropbox API v2 integration using Ktor HttpClient
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/auth/OAuth2Flow.kt` -- `DropboxOAuth2Flow` extension
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/NetworkStorageService.kt` -- The `NetworkStorageService` interface that `DropboxService` implements
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/dropbox/DropboxServiceTest.kt` -- Basic Dropbox service tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/dropbox/DropboxServiceEnhancedTest.kt` -- Enhanced test coverage

### Exercises
1. **Upload/download files** -- Trace through the `downloadFile()` method to understand how Ktor sends the API request with Dropbox-specific headers and processes the binary response.
2. **File listing** -- Examine how `listFiles()` parses the JSON response from Dropbox into `NetworkDocument` objects.
3. **Handle offline sync** -- Review the cache tracking mechanism and design a test scenario where the client goes offline, queues operations, and replays them when connectivity returns.

---

## Video 7.3: Google Drive Integration (15 min)

### Timestamps
- 0:00 Google Drive REST API overview
- 1:30 Scopes and permissions: `drive.file` vs. `drive.readonly`
- 3:00 The `GoogleDriveService` class implementation
- 4:30 File metadata and MIME types
- 6:00 Listing files with query parameters
- 7:30 Downloading file content
- 9:00 Uploading files with multipart upload
- 10:30 Search with Drive query syntax
- 12:00 Google Docs export to supported formats
- 13:30 Rate limiting and quota management
- 14:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/googledrive/GoogleDriveService.kt` -- Google Drive REST API integration
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/googledrive/GoogleDriveServiceTest.kt` -- Service tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/googledrive/GoogleDriveServiceEnhancedTest.kt` -- Enhanced test coverage

### Exercises
1. **Compare with Dropbox** -- Compare `GoogleDriveService` and `DropboxService` implementations: identify similarities in the `NetworkStorageService` interface and differences in API specifics.
2. **Test metadata handling** -- Write a test that verifies Google Drive file metadata (MIME type, modified date, size) maps correctly to `NetworkDocument` fields.
3. **Rate limiting** -- Review how the service handles 429 (rate limit) responses and implement an exponential backoff test.

---

## Videos 7.4-7.12: Additional Protocols

### Video 7.4: OneDrive / Microsoft Graph API (12 min)

#### Timestamps
- 0:00 Microsoft Graph API overview
- 1:30 Azure AD OAuth2 flow differences
- 3:00 `OneDriveService` implementation walkthrough
- 5:00 File operations via Graph endpoints
- 7:00 SharePoint integration considerations
- 9:00 Delta queries for efficient sync
- 11:00 Summary

#### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/onedrive/OneDriveService.kt`
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/onedrive/OneDriveServiceTest.kt`
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/onedrive/OneDriveServiceEnhancedTest.kt`

#### Exercises
1. **Trace the Graph API** -- Follow a `listFiles()` call through `OneDriveService` and identify the Graph API endpoint and response parsing.

---

### Video 7.5: FTP Protocol Implementation (12 min)

#### Timestamps
- 0:00 FTP protocol basics: commands, data channels
- 2:00 `FtpService` class: connecting with credentials
- 4:00 Directory listing and file transfer
- 6:00 Active vs. passive mode
- 8:00 FTP over TLS (FTPS)
- 10:00 Handling timeouts and reconnection
- 11:30 Summary

#### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/ftp/FtpService.kt`
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/ftp/FtpServiceTest.kt`
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/ftp/FtpServiceEnhancedTest.kt`

#### Exercises
1. **Compare FTP and SFTP** -- Read both `FtpService.kt` and `SftpService.kt`, then list the key architectural differences.

---

### Video 7.6: SFTP with SSH Key Authentication (12 min)

#### Timestamps
- 0:00 SFTP vs. FTP: security differences
- 2:00 SSH key-based authentication
- 4:00 `SftpService` implementation
- 6:00 Key management and platform-specific storage
- 8:00 File operations over SFTP
- 10:00 Handling host key verification
- 11:30 Summary

#### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/sftp/SftpService.kt`
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/sftp/SftpServiceTest.kt`
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/sftp/SftpServiceEnhancedTest.kt`

---

### Video 7.7: WebDAV Protocol (10 min)

#### Timestamps
- 0:00 WebDAV protocol overview
- 2:00 `WebDavService` implementation
- 4:00 PROPFIND and PROPPATCH operations
- 6:00 Lock/unlock for concurrent editing
- 8:00 Nextcloud and ownCloud compatibility
- 9:30 Summary

#### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/webdav/WebDavService.kt`
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/webdav/WebDavServiceTest.kt`
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/webdav/WebDavServiceEnhancedTest.kt`

---

### Video 7.8: SMB/CIFS Protocol (10 min)

#### Timestamps
- 0:00 SMB protocol and network shares
- 2:00 `SmbService` implementation
- 4:00 Authentication with domain credentials
- 6:00 File operations on network shares
- 8:00 Windows network discovery
- 9:30 Summary

#### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/smb/SmbService.kt`
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/smb/SmbServiceTest.kt`
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/smb/SmbServiceEnhancedTest.kt`

---

### Video 7.9: Git-based Storage (10 min)

#### Timestamps
- 0:00 Using Git as a storage backend
- 2:00 `GitService` implementation
- 4:00 Clone, pull, commit, push operations
- 6:00 Merge conflict detection and resolution
- 8:00 Partial clone and sparse checkout for large repos
- 9:30 Summary

#### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/git/GitService.kt`
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/git/GitServiceTest.kt`
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/git/GitServiceEnhancedTest.kt`

---

### Video 7.10: Sync Conflict Resolution Algorithms (15 min)

#### Timestamps
- 0:00 Why conflicts happen in distributed editing
- 2:00 Last-write-wins vs. merge strategies
- 4:00 Three-way merge for text files
- 6:00 Operational transform overview
- 8:00 Conflict detection: comparing remote vs. local hashes
- 10:00 User-facing conflict resolution UI
- 12:00 Automatic resolution for non-conflicting changes
- 14:00 Summary

#### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/SyncStatus.kt` -- Sync status tracking
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/NetworkOperation.kt` -- Operation tracking

---

### Video 7.11: Offline Queue and Retry Logic (12 min)

#### Timestamps
- 0:00 Offline-first architecture principles
- 2:00 Operation queue design
- 4:00 Retry strategies: exponential backoff, jitter
- 6:00 Network connectivity detection
- 8:00 Queue persistence across app restarts
- 10:00 Conflict resolution when reconnecting
- 11:30 Summary

#### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/OperationStatus.kt` -- Operation lifecycle states
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/OperationType.kt` -- Types of network operations
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/NetworkStorageError.kt` -- Error types
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/NetworkErrorHandlingTest.kt` -- Error handling tests

---

### Video 7.12: End-to-End Encryption for Cloud Storage (12 min)

#### Timestamps
- 0:00 Why encrypt files before uploading
- 2:00 Encryption key management
- 4:00 AES-256 encryption/decryption
- 6:00 Platform-specific crypto APIs
- 8:00 Key derivation from user passwords
- 10:00 Testing encrypted upload/download cycle
- 11:30 Summary

#### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/platform/SecureStorage.kt` -- Platform-specific secure storage
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/platform/SecureStorageTest.kt` -- Secure storage tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/platform/SecureStorageIntegrationTest.kt` -- Integration tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/security/InputValidationSecurityTest.kt` -- Security validation tests
