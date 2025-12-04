# Network Protocols Quick Start Guide

**Date**: 2025-12-04  
**Feature**: Network Protocols Implementation  
**Purpose**: Developer onboarding and implementation examples

## Overview

This guide helps developers get started with implementing network protocol support in Yole. The network module provides a unified interface for accessing documents across various storage systems while maintaining Yole's offline-first architecture.

## Prerequisites

### Development Environment
- Kotlin 2.1.0 or later
- Android Studio Arctic Fox or later (for Android development)
- IntelliJ IDEA 2023.3 or later (for Desktop development)
- Xcode 14.0 or later (for iOS development)
- Git for version control

### Required Knowledge
- Kotlin Multiplatform Mobile (KMP) development
- Coroutines and Flow
- Network programming concepts
- Platform-specific development (Android/iOS/Desktop/Web)

## Project Structure

The network module follows Yole's KMP structure:

```
shared/src/commonMain/kotlin/digital/vasic/yole/network/
├── common/              # Shared infrastructure
├── protocols/           # Protocol implementations
├── auth/                # Authentication handling
├── cache/               # Caching system
└── sync/                # Synchronization logic
```

## Getting Started

### 1. Setting Up the Development Environment

```bash
# Clone the repository
git clone https://github.com/your-org/yole.git
cd yole

# Checkout the network protocols branch
git checkout 002-network-protocols

# Build the project
./gradlew build

# Run tests
./gradlew test
```

### 2. Understanding the Core Components

#### NetworkStorageService
The main interface for all network operations:

```kotlin
interface NetworkStorageService {
    suspend fun testConnection(config: StorageConfig): Result<Boolean>
    suspend fun listFiles(storageId: String, path: String): Result<List<NetworkDocument>>
    suspend fun downloadDocument(document: NetworkDocument, localPath: String): Result<NetworkOperation>
    suspend fun uploadDocument(storageId: String, localPath: String, remotePath: String): Result<NetworkOperation>
    // ... other methods
}
```

#### Storage Configuration
Each protocol has specific configuration:

```kotlin
// WebDAV Configuration
val webdavConfig = StorageConfig.WebDavConfig(
    serverUrl = "https://cloud.example.com/webdav",
    username = "user@example.com",
    password = "encrypted-password",
    connectionTimeout = 30.seconds
)

// Cloud Storage Configuration
val cloudConfig = StorageConfig.CloudConfig(
    provider = CloudProvider.GOOGLE_DRIVE,
    accessToken = "oauth-token",
    refreshToken = "refresh-token",
    expiresAt = Instant.now().plus(1.hours)
)
```

### 3. Basic Usage Examples

#### Connecting to Network Storage

```kotlin
class NetworkStorageManager {
    private val storageService: NetworkStorageService
    
    suspend fun connectToStorage(config: StorageConfig): Result<String> {
        // Test connection
        val testResult = storageService.testConnection(config)
        if (!testResult.isSuccess) {
            return Result.failure(testResult.exceptionOrNull()!!)
        }
        
        // Authenticate
        val authResult = storageService.authenticate(config)
        if (!authResult.isSuccess) {
            return Result.failure(authResult.exceptionOrNull()!!)
        }
        
        // Save configuration
        return saveStorageConfiguration(config)
    }
}
```

#### Listing Files

```kotlin
suspend fun listRemoteFiles(storageId: String, path: String = "/"): Result<List<NetworkDocument>> {
    return storageService.listFiles(storageId, path).map { documents ->
        documents.filter { !it.isDirectory }
    }
}
```

#### Downloading a Document

```kotlin
suspend fun downloadRemoteDocument(
    storageId: String,
    remotePath: String,
    localPath: String
): Result<NetworkOperation> {
    // Get document metadata
    val document = storageService.getDocument(storageId, remotePath)
        .getOrElse { return Result.failure(it) }
    
    // Download with progress tracking
    return storageService.downloadDocument(document, localPath)
}
```

#### Monitoring Operations

```kotlin
class NetworkOperationMonitor {
    private val operationFlow = storageService.getOperationStream()
    
    fun startMonitoring() {
        operationFlow.onEach { operation ->
            when (operation.status) {
                OperationStatus.RUNNING -> {
                    // Update UI with progress
                    updateProgress(operation.progress)
                }
                OperationStatus.COMPLETED -> {
                    // Notify completion
                    notifyCompletion(operation.id)
                }
                OperationStatus.FAILED -> {
                    // Handle error
                    handleError(operation.errorMessage)
                }
                else -> { /* Other states */ }
            }
        }.launchIn(scope)
    }
}
```

### 4. Implementing a New Protocol

#### Step 1: Create Protocol Implementation

```kotlin
class CustomProtocolService(
    private val httpClient: HttpClient,
    private val authManager: AuthManager
) : NetworkStorageService {
    
    override suspend fun testConnection(config: StorageConfig): Result<Boolean> {
        return try {
            // Implement protocol-specific connection test
            val customConfig = config as StorageConfig.CustomConfig
            val response = httpClient.head(customConfig.serverUrl)
            Result.success(response.status.isSuccess())
        } catch (e: Exception) {
            Result.failure(NetworkStorageError.ConnectionError(e.message, e))
        }
    }
    
    override suspend fun listFiles(storageId: String, path: String): Result<List<NetworkDocument>> {
        // Implement protocol-specific file listing
        TODO("Implement custom protocol file listing")
    }
    
    // Implement other required methods...
}
```

#### Step 2: Register Protocol

```kotlin
object NetworkServiceFactory {
    fun createService(config: StorageConfig): NetworkStorageService {
        return when (config) {
            is StorageConfig.WebDavConfig -> WebDavService(config)
            is StorageConfig.FtpConfig -> FtpService(config)
            is StorageConfig.CustomConfig -> CustomProtocolService(config)
            // ... other protocols
        }
    }
}
```

#### Step 3: Add Tests

```kotlin
class CustomProtocolServiceTest {
    private lateinit var mockService: MockNetworkStorageService
    private lateinit var service: CustomProtocolService
    
    @Before
    fun setup() {
        mockService = MockNetworkStorageService()
        service = CustomProtocolService(mockHttpClient, mockAuthManager)
    }
    
    @Test
    fun `test connection success`() = runTest {
        // Arrange
        val config = StorageConfig.CustomConfig("https://example.com")
        mockService.setConnectionResult(true)
        
        // Act
        val result = service.testConnection(config)
        
        // Assert
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
        mockService.verifyConnectionAttempted()
    }
}
```

### 5. Platform-Specific Implementation

#### Android Integration

```kotlin
// AndroidMain
actual class PlatformSecureStorage : SecureStorage {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore")
    
    actual suspend fun store(key: String, value: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // Use Android Keystore for secure storage
                val secretKey = keyStore.getOrCreateKey(key)
                val encrypted = encryptWithKey(value, secretKey)
                // Store encrypted value in SharedPreferences
                storeEncrypted(key, encrypted)
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
```

#### iOS Integration

```kotlin
// IosMain
actual class PlatformSecureStorage : SecureStorage {
    actual suspend fun store(key: String, value: String): Result<Boolean> {
        return withContext(Dispatchers.Default) {
            try {
                // Use iOS Keychain for secure storage
                val query = mapOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrAccount to key,
                    kSecValueData to value.encodeToByteArray()
                )
                val status = SecItemAdd(query.toCFDictionary(), null)
                Result.success(status == errSecSuccess)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
```

### 6. Testing Best Practices

#### Unit Testing

```kotlin
@Test
fun `upload document with progress tracking`() = runTest {
    // Arrange
    val localPath = "/tmp/test.txt"
    val remotePath = "/remote/test.txt"
    val mockService = MockNetworkStorageService()
    mockService.setUploadResult(true, delay = 2.seconds)
    
    var progressUpdates = 0
    val operationFlow = mockService.getOperationStream()
    
    // Act
    val operation = mockService.uploadDocument("storage-1", localPath, remotePath)
    operationFlow.onEach { progressUpdates++ }.launchIn(this)
    
    // Assert
    assertTrue(operation.isSuccess)
    assertTrue(progressUpdates > 0) // Progress was reported
    mockService.verifyUploadAttempted(remotePath)
}
```

#### Integration Testing

```kotlin
@Test
fun `end to end sync with real server`() = runTest {
    // This test requires a real test server
    val config = StorageConfig.WebDavConfig(
        serverUrl = "https://test-server.com/webdav",
        username = "test-user",
        password = "test-password"
    )
    
    val service = WebDavService(config)
    
    // Test connection
    assertTrue(service.testConnection(config).isSuccess)
    
    // List files
    val files = service.listFiles("storage-1", "/")
    assertTrue(files.isSuccess)
    
    // Upload test file
    val testFile = createTempFile()
    testFile.writeText("Test content")
    val upload = service.uploadDocument("storage-1", testFile.path, "/test.txt")
    assertTrue(upload.isSuccess)
}
```

## Common Patterns

### Error Handling

```kotlin
suspend fun safeNetworkOperation(operation: suspend () -> Result<Unit>): Result<Unit> {
    return try {
        operation()
    } catch (e: NetworkStorageError) {
        // Handle known network errors
        when (e) {
            is NetworkStorageError.ConnectionError -> {
                // Retry with exponential backoff
                retryWithBackoff { operation() }
            }
            is NetworkStorageError.AuthenticationError -> {
                // Refresh credentials
                refreshCredentialsAndRetry()
            }
            else -> Result.failure(e)
        }
    } catch (e: Exception) {
        // Wrap unknown errors
        Result.failure(NetworkStorageError.UnknownError(e.message, e))
    }
}
```

### Caching Strategy

```kotlin
class DocumentCache(
    private val memoryCache: LruCache<String, CacheEntry>,
    private val diskCache: DiskCache
) {
    suspend fun get(documentId: String): Result<NetworkDocument?> {
        // Check memory cache first
        val memoryEntry = memoryCache.get(documentId)
        if (memoryEntry != null && !memoryEntry.isExpired()) {
            return Result.success(memoryEntry.document)
        }
        
        // Check disk cache
        val diskEntry = diskCache.get(documentId)
        if (diskEntry != null && !diskEntry.isExpired()) {
            // Load into memory cache
            memoryCache.put(documentId, diskEntry)
            return Result.success(diskEntry.document)
        }
        
        return Result.success(null) // Not cached
    }
}
```

### Background Sync

```kotlin
class SyncManager(
    private val networkService: NetworkStorageService,
    private val cacheManager: CacheManager
) {
    fun startBackgroundSync() {
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    // Sync all configured storages
                    val storages = getActiveStorages()
                    storages.forEach { storage ->
                        syncStorage(storage.id)
                    }
                } catch (e: Exception) {
                    // Log error but continue syncing
                    logger.error("Sync failed", e)
                }
                
                // Wait before next sync
                delay(SYNC_INTERVAL)
            }
        }
    }
}
```

## Performance Tips

1. **Use streaming for large files** to minimize memory usage
2. **Implement connection pooling** to reduce connection overhead
3. **Cache metadata** to avoid unnecessary network calls
4. **Batch operations** where possible to improve efficiency
5. **Use appropriate timeouts** to balance responsiveness and reliability

## Security Considerations

1. **Always use HTTPS/TLS** for network communications
2. **Store credentials securely** using platform keychains
3. **Validate certificates** to prevent MITM attacks
4. **Implement token refresh** for OAuth2-based services
5. **Sanitize inputs** to prevent injection attacks

## Getting Help

- **API Documentation**: See `/contracts/api-contract.md`
- **Data Model**: See `data-model.md`
- **Architecture**: See Yole's `ARCHITECTURE.md`
- **Issues**: File bug reports in the project repository
- **Discussions**: Use GitHub Discussions for questions

## Contributing

1. Fork the repository
2. Create a feature branch from `002-network-protocols`
3. Implement your changes with tests
4. Ensure all tests pass
5. Submit a pull request with detailed description

## Next Steps

After completing this guide:
1. Explore the existing protocol implementations
2. Run the test suite to understand the testing approach
3. Start implementing your specific protocol or feature
4. Join the development discussions for ongoing coordination