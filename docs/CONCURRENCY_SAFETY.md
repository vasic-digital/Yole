# Concurrency Safety Architecture

*Last updated: 2026-03-17*

## 1. Overview

The Yole project is built on **Kotlin Multiplatform (KMP)** where shared business logic must run on multiple platforms: JVM (Android), iOS, Web (Wasm), and Desktop. This requires strict adherence to KMP-compatible concurrency patterns.

**Critical Constraint**: The JVM-specific `java.util.concurrent.*` package is unavailable in `commonMain` and `commonTest` modules. All concurrency must use **`kotlinx.coroutines`** primitives, which are available across all platforms.

### Why This Matters

- **Java Thread APIs are unavailable** in commonMain (iOS, Wasm, and Desktop don't have native threads)
- **System time functions vary by platform** (no `System.currentTimeMillis()`)
- **Memory management differs** between platforms (no direct GC control)
- **Proper cleanup is essential** to prevent memory leaks across platforms

---

## 2. Thread Safety Patterns Used

### 2.1 Mutex + withLock (Preferred Pattern)

**Use for**: Protecting mutable shared state in suspend contexts

```kotlin
private val mutex = Mutex()
private var sharedState: Int = 0

suspend fun updateState(value: Int) {
    mutex.withLock {
        sharedState = value  // Thread-safe
    }
}
```

**Advantages**:
- Works on all platforms (JVM, iOS, Wasm, Desktop)
- Suspend-friendly (doesn't block threads)
- Strongly-typed, transparent ownership
- Prevents deadlocks with timeouts

**Files using this pattern**:
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/RateLimiting.kt` - All rate limiters
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/LazyLoading.kt` - Lazy document loaders
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/config/NetworkStorageConfigService.kt` - Network configuration
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/dropbox/DropboxService.kt` - stateMutex for `_isConnected`
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/googledrive/GoogleDriveService.kt` - stateMutex for `_isConnected`
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/onedrive/OneDriveService.kt` - stateMutex for `_isConnected`
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/git/GitService.kt` - stateMutex for `_isConnected`
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/webdav/WebDavService.kt` - stateMutex for `_isConnected`
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/smb/SmbService.kt` - stateMutex for `_isConnected`
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/ftp/FtpService.kt` - stateMutex for `_isConnected`
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/sftp/SftpService.kt` - stateMutex for `_isConnected`

---

### 2.2 Semaphore (Concurrency Control)

**Use for**: Limiting concurrent operations

```kotlin
val semaphore = Semaphore(maxConcurrent = 5)

suspend fun executeWithLimit(operation: suspend () -> T): T {
    semaphore.acquire()
    return try {
        operation()
    } finally {
        semaphore.release()
    }
}
```

**Used in**:
- `RateLimiter` class for concurrent operation limiting
- `RateLimitedStorageService` decorator — wraps any `NetworkStorageService` to limit concurrent network operations (default: 4 concurrent)

**Guarantees**: At most `maxConcurrent` coroutines active simultaneously

---

### 2.3 @Volatile (Lazy Initialization Caches)

**Use for**: Simple immutable-after-first-write caches

```kotlin
@Volatile
private var _cachedHtml: String? = null

fun toHtml(lightMode: Boolean = true): String {
    return _cachedHtml ?: run {
        val generated = generateHtml()
        _cachedHtml = generated
        generated
    }
}
```

**Files using this pattern**:
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextParser.kt` - ParsedDocument HTML caches
- Network services - HTML caches (note: `_isConnected` is now protected by `stateMutex.withLock` in all 8 services)

**Safety Guarantee**:
- First write is atomic
- Subsequent reads are lock-free
- Acceptable if cache generation is idempotent

---

### 2.4 synchronized(lock) (Legacy KMP Pattern)

**Use for**: Registry operations requiring atomic check-then-act

```kotlin
object ParserRegistry {
    private val lock = Any()
    private val parsers = mutableMapOf<String, TextParser>()

    fun register(parser: TextParser) {
        synchronized(lock) {
            if (parsers.containsKey(parser.supportedFormat.id)) {
                throw IllegalArgumentException("Already registered")
            }
            parsers[parser.supportedFormat.id] = parser
        }
    }
}
```

**Files using this pattern**:
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextParser.kt` - ParserRegistry

**Note**: `synchronized` works on all KMP platforms but should be preferred only for short, non-suspend operations.

---

### 2.5 SupervisorJob (Structured Concurrency & Cleanup)

**Use for**: Managing coroutine scope lifetime and cleanup

```kotlin
class FlowLazyLoader<T> {
    private val parentJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + parentJob)

    fun cleanup() {
        parentJob.cancel()  // Cancels all children
        _content.value = emptyList()
    }
}
```

**Files using this pattern**:
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/LazyLoading.kt` - FlowLazyLoader

**Safety Guarantee**:
- All spawned coroutines are cancelled on cleanup
- Prevents resource leaks
- Allows graceful shutdown

---

### 2.6 StateFlow / MutableStateFlow (Observable State)

**Use for**: Reactive state that UI can observe

```kotlin
private val _configuredStorages = MutableStateFlow<List<NetworkStorage>>(emptyList())
val configuredStorages: StateFlow<List<NetworkStorage>> = _configuredStorages

// Thread-safe emissions
suspend fun addStorage(config: StorageConfig) {
    mutex.withLock {
        // validate...
        val updated = _configuredStorages.value + newStorage
        _configuredStorages.value = updated
    }
}
```

**Files using this pattern**:
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/config/NetworkStorageConfigService.kt` - Network storage state

**Safety Note**: StateFlow mutations must be protected by Mutex if accessed from multiple coroutines

---

## 3. Key Components

### 3.1 RateLimiting.kt

Located: `shared/src/commonMain/kotlin/digital/vasic/yole/util/RateLimiting.kt`

#### RateLimiter
- **Purpose**: Limits concurrent operations
- **Thread Safety**: Semaphore + Mutex for state
- **API**:
  - `execute(operation)` - Acquire permit, execute, release permit
  - `executeWithTimeout(timeout, operation)` - Timeout variant
  - `getActiveCount()` - Get active operations
  - `getQueueLength()` - Get queued operations

#### TokenBucket
- **Purpose**: Token bucket rate limiting with burst support
- **Thread Safety**: Mutex for token refill and acquisition
- **Uses**: `Clock.System.now()` for KMP-compatible timing (NOT System.currentTimeMillis())
- **API**:
  - `tryAcquire()` - Non-blocking token acquisition
  - `acquire()` - Suspending token acquisition
  - `getAvailableTokens()` - Current token count

#### AdaptiveRateLimiter
- **Purpose**: Self-tuning rate limiter based on success/failure
- **Thread Safety**: Mutex for adjusting rate
- **Behavior**: Increases rate on sustained success (>10 successes), decreases on repeated failures (>=3 failures)

#### OperationThrottler
- **Purpose**: Per-operation-ID throttling with time windows
- **Thread Safety**: Mutex for operation tracking
- **API**:
  - `tryThrottle(operationId)` - Check if operation is allowed within time window
  - `clear(operationId)` - Reset throttling state

---

### 3.2 LazyLoading.kt

Located: `shared/src/commonMain/kotlin/digital/vasic/yole/util/LazyLoading.kt`

#### LazyDocumentLoader<T>
- **Purpose**: Chunk-based lazy loading for large documents
- **Thread Safety**: Mutex for chunk cache and loading state tracking
- **Key Pattern**: Double-checked locking to prevent duplicate chunk loads
  ```kotlin
  suspend fun getChunk(index: Int): T? {
      mutex.withLock {
          _chunks[index]?.let { return it }  // Fast path
      }

      while (true) {
          val shouldLoad = mutex.withLock {
              _chunks[index]?.let { return it }  // Re-check
              if (index in loadingChunks) false else {
                  loadingChunks.add(index); true
              }
          }
          // Load outside lock to prevent deadlock
      }
  }
  ```
- **API**:
  - `getChunk(index)` - Load chunk with concurrent load deduplication
  - `preloadAround(index, range)` - Preload nearby chunks concurrently
  - `clear()` - Free all cached chunks
  - `getMemoryUsage()` - Estimate memory used

#### LazyStringLoader
- **Purpose**: String-specific lazy loader for text documents
- **Thread Safety**: Inherits from LazyDocumentLoader
- **API**:
  - `getLines(startLine, endLine)` - Get range of lines across chunks

#### FlowLazyLoader<T>
- **Purpose**: Flow-based loader with StateFlow for reactive UI integration
- **Thread Safety**:
  - Mutex for content mutations
  - SupervisorJob for scope cleanup
- **Critical**: Must call `cleanup()` to prevent memory leaks
  ```kotlin
  val loader = FlowLazyLoader<String>()
  try {
      loader.content.collect { lines -> display(lines) }
  } finally {
      loader.cleanup()  // CRITICAL: cancels parentJob
  }
  ```
- **API**:
  - `loadMore(items)` - Add items to flow (mutex-protected)
  - `loadChunkAsync(loader)` - Load chunk asynchronously
  - `cleanup()` - Cancel scope and clear state (MUST call)

---

### 3.3 ParserRegistry (in TextParser.kt)

Located: `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextParser.kt`

#### Design
- **Purpose**: Central registry for all text format parsers
- **Thread Safety**: `synchronized(lock)` for all operations
- **Supports**:
  - Eager registration: `register(parser)`
  - Lazy registration: `registerLazy(formatId, factory)`
  - On-demand instantiation of lazy parsers

#### Key Operations
```kotlin
synchronized(lock) {
    // Check for duplicates atomically
    if (parsers.containsKey(formatId) || parserFactories.containsKey(formatId)) {
        throw IllegalArgumentException("Already registered")
    }
    parsers[formatId] = parser
}
```

#### API
- `register(parser)` - Eagerly register parser
- `registerLazy(formatId, factory)` - Lazily register parser
- `getParser(format)` - Get parser with lazy instantiation
- `getParser(formatId)` - Get parser by ID
- `hasParser(format)` - Check without instantiation
- `getAllParsers()` - Get instantiated parsers only
- `getPendingParserCount()` - Monitor lazy loading

---

### 3.4 ParsedDocument (in TextParser.kt)

#### Lazy HTML Caching
- **Purpose**: Defer HTML generation until needed
- **Thread Safety**: `@Volatile` for cache variables
- **Memory Savings**: 50-70% if document never displayed

```kotlin
@Volatile
private var _cachedHtmlLight: String? = null
@Volatile
private var _cachedHtmlDark: String? = null

fun toHtml(lightMode: Boolean = true): String {
    return if (lightMode) {
        _cachedHtmlLight ?: run {
            val generated = ParserRegistry.getParser(format)!!.toHtml(this, lightMode)
            _cachedHtmlLight = generated
            generated
        }
    } else {
        _cachedHtmlDark ?: run {
            val generated = ParserRegistry.getParser(format)!!.toHtml(this, lightMode)
            _cachedHtmlDark = generated
            generated
        }
    }
}
```

#### API
- `toHtml(lightMode)` - Get HTML with lazy generation and caching
- `clearHtmlCache()` - Manually free cached HTML
- `hasHtmlCached(lightMode)` - Check if cached

---

### 3.5 synchronized isConnected() Pattern

All 8 protocol services use `stateMutex.withLock` to protect connection state reads and writes:

```kotlin
// Thread-safe connection state check
suspend fun isConnected(): Boolean {
    return stateMutex.withLock { _isConnected }
}

// Thread-safe connection state update
private suspend fun setConnected(connected: Boolean) {
    stateMutex.withLock { _isConnected = connected }
}
```

This replaces the earlier pattern where `_isConnected` was only protected by `@Volatile`. While `@Volatile` provides visibility, it does not provide atomicity for check-then-act patterns. The `stateMutex.withLock` pattern ensures that connection state checks and operations that depend on them are atomic.

---

### 3.6 @Volatile Fields

`@Volatile` is used for simple immutable-after-first-write caches where races are acceptable because the operation is idempotent:

**Current @Volatile usage**:
- `ParsedDocument._cachedHtmlLight` -- Lazy HTML cache (light mode)
- `ParsedDocument._cachedHtmlDark` -- Lazy HTML cache (dark mode)
- Protocol services `_httpClientAccessed` -- Tracks whether `by lazy` has been triggered

**Not using @Volatile (protected by Mutex instead)**:
- `_isConnected` in all 8 protocol services -- Protected by `stateMutex`
- `activeOperations` maps -- Protected by `operationsMutex`
- `cacheEntries` maps -- Protected by `cacheMutex`

---

### 3.7 StateFlow.update{} for Atomic State Emissions

For reactive state observed by UI, use `StateFlow` with `Mutex` protection:

```kotlin
// Atomic update pattern
suspend fun updateConfiguredStorages(transform: (List<NetworkStorage>) -> List<NetworkStorage>) {
    mutex.withLock {
        _configuredStorages.value = transform(_configuredStorages.value)
    }
}
```

This ensures that read-modify-write cycles on `StateFlow` values are atomic, preventing lost updates when multiple coroutines modify the state concurrently.

---

### 3.8 Network Services

#### Pattern
All 8 network services (DropboxService, FtpService, SftpService, GoogleDriveService, OneDriveService, WebDavService, SmbService, GitService) follow:

```kotlin
class DropboxService {
    private val httpClient by lazy { createHttpClient() }
    @Volatile
    private var isConnected = false
    private val activeOperations = mutableMapOf<Long, NetworkOperation>()
    private val operationsMutex = Mutex()  // Protects activeOperations

    suspend fun trackOperation(operation: NetworkOperation) {
        operationsMutex.withLock {
            activeOperations[operation.id] = operation
        }
    }
}
```

#### Thread Safety Mechanisms
- **httpClient**: `by lazy` for thread-safe first initialization
- **Connection state**: `@Volatile` for lock-free reads
- **Active operations**: Mutex-protected map
- **Timing**: `Clock.System.now()` for all time operations

---

## 4. Concurrency Test Coverage

### 4.1 ConcurrencySafetyTest

Located: `shared/src/commonTest/kotlin/digital/vasic/yole/util/ConcurrencySafetyTest.kt`

**Tests**:
- `testMutexProtectedCounter` - 100 coroutines x 100 increments each = 10,000 total
- `testConcurrentReadWrite` - Mixed 5 writers + 10 readers
- `testConcurrentMapAccess` - 20 coroutines updating separate map keys
- `testChannelThreadSafety` - Unidirectional channel correctness
- `testSuspendingMutexDeadlockPrevention` - Timeout handling

**Verification**: Uses `runTest` (multiplatform test dispatcher) instead of `Thread.sleep()`

---

### 4.2 RaceConditionDetectionTest

Located: Checks for check-then-act races in critical sections

**Pattern Tests**:
- Protected increments with Mutex
- Atomic registration in registries
- Double-checked locking patterns

---

### 4.3 MemoryLeakDetectionTest

Located: `shared/src/commonTest/kotlin/digital/vasic/yole/util/MemoryLeakDetectionTest.kt`

**Checks**:
- FlowLazyLoader scope cancellation
- Resource cleanup on coroutine cancellation
- Null checks to verify GC (since direct GC is unavailable)

---

### 4.4 StressAndIntegrationTest

**High-Load Scenarios**:
- RateLimiter under 100+ concurrent requests
- LazyStringLoader with 10,000+ line documents
- ParserRegistry under concurrent access
- TokenBucket sustained high-frequency acquisitions

---

## 5. KMP Compatibility Rules

### ✅ DO USE

| Pattern | Example | Platform Availability |
|---------|---------|----------------------|
| `Mutex` | `val m = Mutex()` | All platforms |
| `Semaphore` | `val s = Semaphore(5)` | All platforms |
| `delay()` | `delay(100)` | All platforms (suspend only) |
| `launch()` | `launch { ... }` | All platforms |
| `kotlinx.datetime.Clock` | `Clock.System.now()` | All platforms |
| `StateFlow` | `MutableStateFlow<T>()` | All platforms |
| `@Volatile` | `@Volatile var x` | All platforms |
| `synchronized(lock)` | `synchronized(Any())` | All platforms |

### ❌ NEVER USE IN commonMain/commonTest

| ❌ Pattern | Why | Alternative |
|-----------|-----|-------------|
| `java.util.concurrent.*` | Not available on iOS/Wasm | Use `kotlinx.coroutines.sync.*` |
| `java.lang.Thread` | No native threads on all platforms | Use `launch(Dispatchers.Default)` |
| `System.currentTimeMillis()` | Not available on iOS | Use `Clock.System.now().toEpochMilliseconds()` |
| `Thread.sleep()` | Blocks real thread (bad in coroutines) | Use `delay()` |
| `System.gc()` | Not available on all platforms | Rely on GC, design for memory efficiency |
| `java.lang.ref.WeakReference` | Not available on iOS | Test cleanup with null checks |
| `kotlin.concurrent.thread {}` | Creates real thread | Use `launch(Dispatchers.Default)` |
| `org.junit.Test` | Android-only | Use `kotlin.test.Test` |
| `AtomicInteger` | JVM-only | Use Mutex + withLock |

---

## 6. Best Practices

### 6.1 Avoid Deadlocks
- **Use `withLock` instead of manual acquire/release**
- **Never hold a lock while suspending** (unless using `withLock` which is designed for it)
- **Use `SupervisorJob` instead of plain `Job`** to prevent cascading failures

### 6.2 Resource Cleanup
```kotlin
// CORRECT: FlowLazyLoader must be cleaned up
val loader = FlowLazyLoader<String>()
try {
    loader.loadChunkAsync { ... }
} finally {
    loader.cleanup()  // Cancels parentJob
}

// WRONG: Leaks the coroutine scope
val loader = FlowLazyLoader<String>()
loader.loadChunkAsync { ... }
// Never calls cleanup()
```

### 6.3 Lazy Initialization
```kotlin
// CORRECT: Thread-safe with @Volatile
@Volatile
private var cache: String? = null

fun getCached(): String {
    return cache ?: run {
        val generated = expensiveOperation()
        cache = generated
        generated
    }
}

// WRONG: Races possible
private var cache: String? = null
fun getCached(): String {
    if (cache == null) {
        cache = expensiveOperation()  // Race condition
    }
    return cache!!
}
```

### 6.4 Testing
- **Use `runTest` from `kotlin.test`** (not `runBlocking`)
- **Avoid `Thread.sleep()` in tests**
- **Use `advanceUntilIdle()` to complete pending coroutines**

```kotlin
@Test
fun testConcurrency() = runTest {
    val limiter = RateLimiter(maxConcurrent = 2)
    val jobs = (1..10).map {
        launch { limiter.execute { delay(100) } }
    }
    advanceUntilIdle()  // Wait for all jobs
    assertEquals(0, limiter.getActiveCount())
}
```

---

## 7. Migration Guide (Legacy Code)

### If converting code to KMP:

1. **Replace `java.util.concurrent.locks.ReentrantLock`**
   ```kotlin
   // Before (Android-only)
   val lock = ReentrantLock()
   lock.withLock { ... }

   // After (Multiplatform)
   val lock = Mutex()
   lock.withLock { ... }  // Same API!
   ```

2. **Replace `Thread.sleep()`**
   ```kotlin
   // Before
   Thread.sleep(100)

   // After (requires suspend context)
   delay(100)
   ```

3. **Replace `AtomicInteger`**
   ```kotlin
   // Before
   val counter = AtomicInteger(0)
   counter.incrementAndGet()

   // After
   var counter = 0
   mutex.withLock { counter++ }
   ```

4. **Replace timing calls**
   ```kotlin
   // Before
   val ms = System.currentTimeMillis()

   // After
   val ms = Clock.System.now().toEpochMilliseconds()
   ```

---

## Summary

| Component | Pattern | Safety | Use Case |
|-----------|---------|--------|----------|
| **RateLimiting** | Semaphore + Mutex | High | Concurrent request limiting |
| **LazyLoading** | Mutex + double-checked locking | High | Memory-efficient chunk loading |
| **ParserRegistry** | synchronized(lock) | High | Atomic registration |
| **ParsedDocument** | @Volatile | Medium | Lazy HTML caching |
| **Network Services** | Mutex + @Volatile + by lazy | High | Safe concurrent operations |
| **StateFlow** | Mutex + StateFlow | High | Observable reactive state |

All patterns are **Kotlin Multiplatform compatible** and can be safely used across JVM, iOS, Wasm, and Desktop targets.
