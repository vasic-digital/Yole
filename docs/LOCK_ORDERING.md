# Lock Ordering Convention

Yole's network protocol services use multiple `kotlinx.coroutines.sync.Mutex` instances to protect different categories of mutable state. To prevent deadlocks, all code must acquire locks in the order defined below. No code may hold a higher-numbered lock while attempting to acquire a lower-numbered lock.

## Lock Hierarchy

| Priority | Mutex | Protects | Scope |
|----------|-------|----------|-------|
| 1 (outermost) | `scopeMutex` | `serviceScope` lifecycle (creation, cancellation, recreation) | Service-wide |
| 2 | `stateMutex` | Connection state (`_isConnected`, `_rootPath`) | Service-wide |
| 3 | `operationsMutex` | `activeOperations` map (operation metadata tracking) | Service-wide |
| 4 | `syncMutex` | `syncStatusMap` (sync status tracking per file) | Service-wide |
| 5 | `cacheMutex` | `cacheEntries` map (in-memory cache storage) | Service-wide |
| 6 | `pauseFlagsMutex` | `pauseFlags` map (operation pause/resume `StateFlow` flags) | Service-wide |
| 7 (leaf) | `activeJobsMutex` | `activeJobs` map (coroutine `Job` references for cancel/pause) | Service-wide |
| 8 (leaf) | `storageInitMutex` | `SecureStorage` initialization (platform keychain/keystore access) | Platform-specific |

## Rules

1. **Always acquire locks in ascending priority order.** If you hold `stateMutex` (2), you may acquire `operationsMutex` (3) or any higher number, but you must not acquire `scopeMutex` (1).

2. **Leaf locks (7, 8) never hold other locks.** Code holding `activeJobsMutex` or `storageInitMutex` must never attempt to acquire any other mutex. This simplifies reasoning about deadlock freedom.

3. **Minimize lock scope.** Acquire a mutex, perform the minimum necessary state mutation, and release it. Never perform I/O (HTTP requests, file system operations) while holding a lock.

4. **Prefer single-lock operations.** Most operations need only one mutex. Multi-lock acquisition should be rare and always follow the ordering.

## Rationale

### Why `scopeMutex` is outermost (1)

The `serviceScope` manages the lifecycle of all background coroutines. Cancelling or recreating the scope affects everything below it -- active operations, sync jobs, cache updates. Holding the scope lock first ensures that no other operation is in the middle of modifying state that depends on the scope.

Example: `disconnect()` acquires `scopeMutex` first, cancels the scope, then acquires `stateMutex` to set `_isConnected = false`.

### Why `stateMutex` is second (2)

Connection state (`_isConnected`) is checked by nearly every operation before proceeding. It must be consistent with the scope lifecycle (hence after `scopeMutex`) but must be settled before any operations modify the operations map or cache.

### Why `operationsMutex` is third (3)

The active operations map tracks metadata about in-flight operations (upload progress, download status). It depends on the connection being valid (hence after `stateMutex`) and is read/written more frequently than sync or cache state.

### Why `syncMutex` is fourth (4)

Sync status tracking is updated during sync operations, which are a subset of all operations. It depends on the operations map being consistent.

### Why `cacheMutex` is fifth (5)

Cache entries are updated as a side effect of file operations. The cache never needs to inspect the sync status or operations map, but operations that populate the cache may need to update those first.

### Why `pauseFlagsMutex` is sixth (6)

Pause flags control whether an in-progress operation should pause or resume. They are independent of cache and sync state but may need to be checked after updating operations metadata.

### Why `activeJobsMutex` is a leaf (7)

Active jobs are coroutine `Job` references used solely for cancellation and pause signaling. Cancelling a job never requires holding any other lock -- the cancellation propagates through the coroutine machinery independently.

### Why `storageInitMutex` is a leaf (8)

SecureStorage initialization accesses platform-specific credential storage (Android Keystore, iOS Keychain, desktop file-based). This is a one-time initialization that does not interact with any protocol service state.

## Examples

### Correct: Disconnect (multi-lock, ascending order)

```kotlin
suspend fun disconnect(): Result<Unit> {
    // Lock 1: Cancel the scope
    scopeMutex.withLock {
        serviceScope.cancel()
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
    // Lock 2: Update connection state
    stateMutex.withLock {
        _isConnected = false
    }
    // Lock 3: Clear operations
    operationsMutex.withLock {
        activeOperations.clear()
    }
    return Result.success(Unit)
}
```

### Correct: Single-lock cache update

```kotlin
suspend fun addToCache(remotePath: String, priority: Int): Result<Unit> {
    cacheMutex.withLock {
        cacheEntries[remotePath] = CacheEntry(remotePath, priority)
    }
    return Result.success(Unit)
}
```

### Incorrect: Reverse lock order (would deadlock)

```kotlin
// DO NOT DO THIS
suspend fun badExample() {
    operationsMutex.withLock {     // Lock 3
        stateMutex.withLock {      // Lock 2 -- VIOLATION: lower than 3
            // ...
        }
    }
}
```

### Incorrect: I/O under lock (blocks other coroutines)

```kotlin
// DO NOT DO THIS
suspend fun badExample2() {
    stateMutex.withLock {
        val response = httpClient.get(endpoint) // Network I/O under lock
        _isConnected = response.status.isSuccess()
    }
}

// CORRECT: Perform I/O outside the lock
suspend fun correctExample() {
    val response = httpClient.get(endpoint)
    stateMutex.withLock {
        _isConnected = response.status.isSuccess()
    }
}
```

## Mutex Declarations in Protocol Services

All 8 protocol services (DropboxService, GoogleDriveService, OneDriveService, WebDavService, FtpService, SftpService, SmbService, GitService) follow the same pattern:

```kotlin
class ExampleService(...) : NetworkStorageService {
    // Priority 1
    private val scopeMutex = Mutex()
    private var serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Priority 2
    private val stateMutex = Mutex()
    private var _isConnected = false

    // Priority 3
    private val operationsMutex = Mutex()
    private val activeOperations = mutableMapOf<Long, NetworkOperation>()

    // Priority 4
    private val syncMutex = Mutex()
    private val syncStatusMap = mutableMapOf<String, SyncStatus>()

    // Priority 5
    private val cacheMutex = Mutex()
    private val cacheEntries = mutableMapOf<String, CacheEntry>()

    // Priority 6
    private val pauseFlagsMutex = Mutex()
    private val pauseFlags = mutableMapOf<Long, MutableStateFlow<Boolean>>()

    // Priority 7 (leaf)
    private val activeJobsMutex = Mutex()
    private val activeJobs = mutableMapOf<Long, Job>()
}
```

## Additional Lock Entries

### FormatRegistry Lazy Init Lock

The `FormatRegistry.formats` property uses Kotlin's `lazy { }` delegate with `LazyThreadSafetyMode.SYNCHRONIZED` (the default). This is an implicit lock that serializes access to the format list on first initialization. The `isFormatsInitialized` guard allows callers to check initialization state without triggering the lazy.

### StyleSheets Cache Lock

`StyleSheets.styleSheetCache` is a simple `MutableMap<String, String>` protected by the calling pattern (single-threaded access in practice). The `clearCache()` method resets the map on theme changes.

### ParserRegistry synchronized(lock)

`ParserRegistry` uses `synchronized(lock)` (priority: independent, not part of the protocol service lock hierarchy) for:
- `register(parser)` -- Atomic check-for-duplicate then insert
- `registerLazy(formatId, factory)` -- Atomic check then store factory
- `getParser(format)` -- Atomic check then lazy instantiation

This lock is never held while any protocol service lock is held, so there is no ordering constraint with the protocol lock hierarchy.

### DocumentCache Internal Synchronization

`DocumentCache` uses internal synchronization for LRU eviction with cooperative cancellation (`yield()` in long-running eviction loops). This is independent of the protocol service lock hierarchy.

## Verification

Lock ordering is enforced through code review. The `ConcurrencySafetyTest` and `ComprehensiveStressTests` test suites exercise concurrent access patterns to detect ordering violations empirically. The `ContractTestsForProtocols` suite verifies that all 8 protocol services maintain consistent locking behavior. The `ConcurrencyFixesTest` (1006 lines) specifically tests the 10 critical concurrency fixes applied to protocol services.
