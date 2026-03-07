# Resilience Patterns

Yole's network protocol layer employs four resilience patterns to ensure reliable cloud storage integration across unreliable networks. Each pattern addresses a distinct failure mode and can be configured independently per protocol service.

## CircuitBreaker

**Location:** `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/CircuitBreaker.kt`

The circuit breaker prevents cascading failures by tracking consecutive error rates and temporarily blocking operations when a failure threshold is exceeded.

### State Machine

```
     success          failure >= threshold
  +----------+       +------------------+
  |          |       |                  |
  v          |       v                  |
CLOSED ------+----> OPEN ----timeout----> HALF_OPEN
  ^                                        |
  |              success                   |
  +----------------------------------------+
  |              failure                   |
  |          +-----------------------------+
  |          v
  +------- OPEN (reset timer)
```

**States:**

| State | Behavior |
|-------|----------|
| CLOSED | Normal operation. Every failure increments `failureCount`. When `failureCount >= failureThreshold`, transitions to OPEN. |
| OPEN | All operations immediately fail with `CircuitBreakerOpenException`. After `resetTimeout` elapses since the last failure, transitions to HALF_OPEN. |
| HALF_OPEN | A single trial operation is permitted. If it succeeds, transitions to CLOSED (and resets `failureCount`). If it fails, transitions back to OPEN. |

### Configuration

| Parameter | Default | Description |
|-----------|---------|-------------|
| `failureThreshold` | 5 | Number of consecutive failures before opening the circuit |
| `resetTimeout` | 30 seconds | Duration to wait in OPEN before transitioning to HALF_OPEN |
| `name` | `"default"` | Human-readable name for logging and diagnostics |

### Usage in Protocol Services

Each protocol service creates its own circuit breaker instance:

```kotlin
class DropboxService(...) : NetworkStorageService {
    private val circuitBreaker = CircuitBreaker(
        name = "dropbox",
        failureThreshold = 5
    )
}
```

Operations are wrapped with `circuitBreaker.execute { }`, which returns `Result<T>`:

```kotlin
val result = circuitBreaker.execute {
    httpClient.get(endpoint)
}
```

### Monitoring

| Metric | Property | Description |
|--------|----------|-------------|
| Current state | `state` | CLOSED, OPEN, or HALF_OPEN |
| Failure count | `failures` | Consecutive failures since last success |
| Success count | `successes` | Total successful operations |
| Total calls | `calls` | Total operations attempted |

### CancellationException Safety

The circuit breaker rethrows `CancellationException` in `tryExecution()` to maintain coroutine flow transparency. Cancellations are never counted as failures.

---

## ConnectionLimiter

**Location:** `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/ConnectionLimiter.kt`

The connection limiter uses a `kotlinx.coroutines.sync.Semaphore` to cap concurrent operations for a protocol service. Callers suspend (not block) until a permit becomes available.

### Configuration

| Parameter | Default | Description |
|-----------|---------|-------------|
| `maxConcurrent` | 5 | Maximum number of concurrent operations |
| `acquireTimeout` | 30 seconds | Maximum time to wait for a permit before failing with `TimeoutCancellationException` |
| `name` | `"default"` | Human-readable name for diagnostics |

### Usage in Protocol Services

Each protocol service creates its own connection limiter:

```kotlin
class DropboxService(...) : NetworkStorageService {
    private val connectionLimiter = ConnectionLimiter(
        name = "dropbox",
        maxConcurrent = 5
    )
}
```

Operations are wrapped with `connectionLimiter.withConnection { }`:

```kotlin
val documents = connectionLimiter.withConnection {
    httpClient.get(listFilesEndpoint)
}
```

### Monitoring

| Metric | Property | Description |
|--------|----------|-------------|
| Available permits | `availablePermits` | Number of permits not currently held |
| Utilization | `maxConcurrent - availablePermits` | Number of active concurrent operations |

### Non-blocking Guarantee

The semaphore-based design ensures that waiting coroutines suspend rather than blocking threads. This is critical for mobile platforms where blocking the main thread causes ANR (Application Not Responding) errors.

---

## DocumentCache

**Location:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/DocumentCache.kt`

An LRU (Least Recently Used) cache for `ParsedDocument` instances. All operations are protected by a `Mutex` for coroutine safety.

### Configuration

| Parameter | Default | Description |
|-----------|---------|-------------|
| `maxSize` | 100 | Maximum number of cached entries. When exceeded, the least recently accessed entry is evicted. |

### API

| Method | Description |
|--------|-------------|
| `get(key)` | Retrieve a cached document. Returns `null` on miss. Updates hit/miss counters. |
| `put(key, document)` | Store a document. Evicts the LRU entry if `size > maxSize`. |
| `invalidate(key)` | Remove a specific entry. |
| `clear()` | Remove all entries and reset hit/miss counters. |
| `contains(key)` | Check if a key exists without affecting LRU order or hit/miss counters. |

### Monitoring

| Metric | Property | Description |
|--------|----------|-------------|
| Cache size | `size` | Current number of entries |
| Cache hits | `hits` | Total get() calls that returned a value |
| Cache misses | `misses` | Total get() calls that returned null |
| Hit rate | `hitRate` | `hits / (hits + misses)`, 0.0 if no calls yet |

### LRU Eviction

The cache uses `LinkedHashMap` with `accessOrder = true`, which reorders entries on every access. When capacity is exceeded, the first entry (least recently accessed) is removed.

---

## RateLimitedStorageService

**Location:** `shared/src/commonMain/kotlin/digital/vasic/yole/network/RateLimitedStorageService.kt`

A decorator that wraps any `NetworkStorageService` to add semaphore-based concurrency limiting for all network operations. This prevents overwhelming remote servers during bulk operations (e.g., multi-file sync, batch downloads).

### Configuration

| Parameter | Default | Description |
|-----------|---------|-------------|
| `delegate` | (required) | The underlying `NetworkStorageService` to wrap |
| `maxConcurrent` | 4 | Maximum number of concurrent operations |

### Usage

```kotlin
val dropbox = DropboxService(config)
val rateLimited = RateLimitedStorageService(dropbox, maxConcurrent = 4)

// All operations through rateLimited are now concurrency-limited
rateLimited.uploadFile(localPath, remotePath)
```

### Which Operations Are Rate-Limited

| Rate-limited | Not rate-limited |
|-------------|-----------------|
| `connect()`, `disconnect()` | `getActiveOperations()` |
| `listFiles()`, `searchFiles()` | `getRecentChanges()` |
| `downloadFile()`, `uploadFile()` | `getCacheEntries()` |
| `deleteFile()`, `createFolder()` | `getSyncStatus()` |
| `renameFile()`, `moveFile()`, `copyFile()` | `cancelOperation()` |
| `getFileInfo()`, `getStorageInfo()` | `pauseOperation()` |
| `testConnection()`, `exists()` | `resumeOperation()` |
| `syncFile()`, `syncAll()` | `getParentPath()` |
| `addToCache()`, `removeFromCache()`, `clearCache()` | `validatePath()` |
| `getQuotaInfo()` | |

Non-rate-limited operations are either cheap local queries or control-plane operations that should execute immediately (e.g., cancelling an in-progress operation).

---

## Integration with Protocol Services

Each of the 8 protocol services (Dropbox, Google Drive, OneDrive, WebDAV, FTP, SFTP, SMB, Git) integrates these patterns:

1. **CircuitBreaker** -- created per service instance with service-specific name
2. **ConnectionLimiter** -- created per service instance with configurable concurrency
3. **CancellationException safety** -- all catch blocks rethrow `CancellationException`
4. **CoroutineScope lifecycle** -- `serviceScope` is cancelled on reconnect/disconnect to prevent coroutine leaks
5. **Path traversal protection** -- `normalizePath()` resolves `..` segments and enforces root boundary
6. **Query injection protection** -- API query strings are sanitized (single-quote escaping, URL encoding, JSON escaping)

### Resilience Flow for a Typical Operation

```
User Request
    |
    v
RateLimitedStorageService (semaphore gate)
    |
    v
ConnectionLimiter.withConnection { }
    |
    v
CircuitBreaker.execute { }
    |
    v
HTTP Request (Ktor)
    |
    v
Response Handling
    |
    v
DocumentCache.put() (if applicable)
```

---

## Recommended Configurations by Protocol

| Protocol | CircuitBreaker Threshold | ConnectionLimiter Max | RateLimiter Max | Rationale |
|----------|--------------------------|----------------------|-----------------|-----------|
| Dropbox | 5 | 5 | 4 | Dropbox API v2 rate limit: 200 req/15 min |
| Google Drive | 5 | 5 | 4 | Google Drive API: 1000 req/100s |
| OneDrive | 5 | 5 | 4 | Microsoft Graph API: standard throttling |
| WebDAV | 5 | 5 | 4 | Self-hosted; adjust based on server capacity |
| FTP | 3 | 2 | 2 | FTP servers typically limit concurrent connections |
| SFTP | 3 | 3 | 3 | SSH connection overhead; fewer concurrent ops |
| SMB | 3 | 3 | 3 | Network share limits vary by server |
| Git | 5 | 2 | 2 | Git operations are sequential by nature |

---

## Testing

Resilience patterns are covered by dedicated test suites:

- `ResilienceTests.kt` -- 53 tests for CircuitBreaker, ConnectionLimiter, DocumentCache
- `PerformanceMetricsTests.kt` -- 29 tests for timing baselines
- `MonitoringMetricsTests.kt` -- 42 tests for parse time, HTML generation, detection, memory, throughput, lazy loading
- `ComprehensiveStressTests.kt` -- 30 tests for concurrent parsing, cache contention, memory stability
- `ContractTestsForProtocols.kt` -- 89 tests for 8 protocols x 10+ contracts
- `SafetyFixesTest.kt` -- 92 safety tests for CancellationException, injection, path traversal
