<!--
SPDX-FileCopyrightText: 2024-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Performance Tuning Guide

This document covers techniques for optimizing Yole's performance across format parsing, network operations, memory management, and platform-specific concerns.

---

## Format Parser Optimization

### Lazy HTML Generation

Yole's `ParsedDocument` uses lazy HTML caching: the HTML representation is generated only on the first call to `toHtml()`, and subsequent calls return the cached result. This avoids unnecessary work when documents are parsed but not previewed.

**Impact**: For documents that are never previewed (e.g., background format detection), HTML generation is skipped entirely, saving both CPU time and memory allocation.

**Tuning tip**: If you are building a feature that processes many documents (batch operations, search indexing), avoid calling `toHtml()` unless the HTML output is actually needed.

### FormatRegistry Lazy Initialization

The `FormatRegistry.formats` list uses Kotlin's `lazy { createFormats() }` delegate. This defers the creation of all 17 `TextFormat` entries until the first time the list is accessed.

**Benchmarks**:

| Metric | Eager | Lazy | Improvement |
|--------|-------|------|-------------|
| Startup time | 30-50ms | 0ms (deferred) | 100% startup savings |
| First access | 0ms | 30-50ms | Deferred to first use |
| Subsequent access | 0ms | 0ms | Cached |

Use `FormatRegistry.isFormatsInitialized` to check initialization state without triggering it.

### Parser Lazy Registration

`ParserInitializer.registerAllParsersLazy()` registers factory lambdas instead of parser instances. Each parser is instantiated on first `getParser()` call.

**Benchmarks**:

| Metric | Eager (registerAllParsers) | Lazy (registerAllParsersLazy) | Improvement |
|--------|--------------------------|-------------------------------|-------------|
| Registration time | 30-50ms | 1-2ms | 95% faster |
| Memory at startup | 17 parser instances | 0 instances | Deferred |
| First-use latency | 0ms | 1-3ms per parser | Imperceptible |

### Lazy Document Loading

The `LazyDocumentLoader<T>` and `FlowLazyLoader<T>` utilities (from Concurrency-KMP, re-exported via `digital.vasic.yole.util.LazyLoading`) defer expensive initialization until first access:

```kotlin
val loader = LazyDocumentLoader<ParsedDocument> {
    parser.parse(rawContent)
}
// No work done yet
val doc = loader.get()  // Parsing happens here (once)
val doc2 = loader.get() // Returns cached result
```

**Impact**: Reduces startup time by deferring parsing of documents that may never be opened.

### Format Detection Optimization

`FormatRegistry.detectByExtension()` is O(1) -- it uses a map lookup by file extension. `detectByContent()` tests regex patterns in priority order and short-circuits on the first match.

**Tuning tip**: If you know the file extension, always use `detectByExtension()` rather than `detectByContent()`. Content detection is more expensive because it applies multiple regex patterns.

### Parser-Specific Notes

| Format | Complexity | Notes |
|--------|-----------|-------|
| Markdown | High | Uses Flexmark with 16+ extensions; large documents may be slow |
| LaTeX | Medium | Regex-based; performance scales linearly with document size |
| CSV | Low | Simple split-based parsing; very fast even for large files |
| Todo.txt | Low | Line-by-line parsing; fast |
| Jupyter | Medium | JSON deserialization; depends on notebook size |
| Plain Text | Minimal | Almost no processing overhead |

**Tuning tip for Markdown**: If preview performance is slow on very large Markdown documents (10,000+ lines), consider splitting the document into smaller files.

---

## Network Protocol Tuning

### Connection Pooling

Yole creates an `HttpClient` per service instance (Dropbox, Google Drive, OneDrive, WebDAV, Git). The Ktor HTTP client maintains an internal connection pool, so multiple requests to the same server reuse connections.

**Tuning tip**: Avoid creating and destroying service instances frequently. Create the service once and reuse it for all operations.

### Rate Limiting

Yole includes three levels of rate limiting (from RateLimiter-KMP):

| Component | Purpose | Default Configuration |
|-----------|---------|----------------------|
| `TokenBucket` | Fixed-rate token bucket limiter | Configurable tokens per interval |
| `AdaptiveRateLimiter` | Adjusts rate based on server responses (429/503) | Automatically backs off on errors |
| `OperationThrottler` | Limits concurrent operations per protocol | Configurable max concurrent |

**Tuning tips**:
- If you see frequent `429 Too Many Requests` errors, the rate limiter will automatically back off. No manual tuning is needed.
- For servers with known rate limits (e.g., Dropbox development apps: ~100 requests/minute), configure the `TokenBucket` with appropriate values.
- The `OperationThrottler` prevents flooding a server with parallel requests. The default concurrency limit is appropriate for most servers.

### Circuit Breaker

The `CircuitBreaker` prevents cascading failures by temporarily blocking operations after repeated errors:

| Parameter | Default | Description |
|-----------|---------|-------------|
| `failureThreshold` | 5 | Consecutive failures before opening the circuit |
| `resetTimeout` | 30 seconds | Time before allowing a trial operation |

**States**: CLOSED (normal) -> OPEN (blocked) -> HALF_OPEN (trial) -> CLOSED/OPEN

**Tuning tip**: For unreliable servers, increase the `failureThreshold` to tolerate more transient errors before tripping. For servers that take a long time to recover, increase the `resetTimeout`.

### Connection Limiter

The `ConnectionLimiter` uses a semaphore to cap concurrent connections:

| Parameter | Default | Description |
|-----------|---------|-------------|
| `maxConcurrent` | 5 | Maximum parallel operations |
| `acquireTimeout` | 30 seconds | Maximum wait time for a permit |

**Tuning tips**:
- For high-latency connections (satellite, VPN), increase `acquireTimeout` to prevent premature failures.
- For servers that struggle with parallel connections, reduce `maxConcurrent` to 2 or 3.
- For fast local servers (WebDAV on LAN), you can increase `maxConcurrent` to 10 or more.

### Timeout Configuration

Each storage config has a `connectionTimeout` field (default: 30,000 ms). WebDAV also has a `readTimeout` (default: 60,000 ms).

**Tuning tips**:
- For fast local networks, reduce timeouts to 10,000 ms for quicker error detection.
- For slow or unreliable connections, increase to 60,000-120,000 ms.
- Upload/download operations stream data progressively and do not depend on the read timeout for the entire transfer.

---

## Memory Management

### Document Cache

Yole caches parsed documents in memory. The `CacheEntry` data class tracks:
- `size` -- cached file size in bytes
- `accessCount` -- how often the entry has been accessed
- `accessFrequency` -- accesses per day (used for eviction priority)
- `priority` -- manual priority setting (higher = less likely to be evicted)
- `isPinned` -- pinned entries are never evicted

**Cache eviction criteria** (in order of preference):
1. Expired entries (`expiresAt` has passed)
2. Unpinned, unused entries with the lowest `priority`
3. Least recently accessed entries (LRU when priorities are equal)

**Tuning tips**:
- Pin frequently accessed documents to prevent eviction
- Set a TTL (time-to-live) for documents that change frequently on the server
- Monitor cache size with `getCacheUsage()` from `NetworkStorageDatabase`
- Call `clearCache()` if memory pressure is high

### GC Pressure

Common sources of GC pressure in Yole:

1. **Large ParsedDocument instances** -- Each includes raw content, parsed content, and optional HTML cache
2. **File listing results** -- Large directories can produce thousands of `NetworkDocument` instances
3. **Progress emission** -- Upload/download flows emit many `NetworkOperation` copies

**Tuning tips**:
- Avoid holding references to `ParsedDocument` instances that are no longer displayed
- Use pagination when listing large directories (Dropbox and OneDrive support cursor-based pagination)
- Collect upload/download flows with `.collect {}` rather than `.toList()` to avoid accumulating all intermediate states

---

## Platform-Specific Tips

### Android

**Battery optimization**:
- Yole defers background sync when the device is in Doze mode
- Set `autoSync = false` on storage configurations that do not need real-time sync
- Use Wi-Fi-only sync for large files to preserve mobile data and battery

**Memory constraints**:
- Android devices have limited heap space (typically 256-512 MB for an app)
- Avoid opening multiple large documents simultaneously
- The OS may kill background processes; cached documents are preserved on disk

**Storage access**:
- Use the Android Storage Access Framework (SAF) for accessing files outside the app's sandbox
- Granting `MANAGE_EXTERNAL_STORAGE` is not required for normal operation

### Desktop (JVM)

**Memory allocation**:
- The JVM defaults to a relatively small heap size
- For large document collections, increase the JVM heap:
  ```bash
  ./gradlew :desktopApp:run -Dorg.gradle.jvmargs="-Xmx2g"
  ```
- Monitor memory usage with VisualVM or JFR (Java Flight Recorder)

**Concurrent operations**:
- Desktop has more CPU cores and memory than mobile; increase `ConnectionLimiter.maxConcurrent` for faster batch operations
- JVM thread pool sizing is handled by kotlinx.coroutines (`Dispatchers.IO` scales to 64 threads by default)

**File watching**:
- Desktop supports file system watching for automatic reload when external editors modify files
- This uses `java.nio.file.WatchService` and is efficient for small numbers of watched directories

### iOS

**Background execution**:
- iOS limits background execution time to ~30 seconds
- Long sync operations should be registered as background tasks with `BGTaskScheduler`
- File downloads/uploads should use `URLSession` background transfer capabilities

**Memory constraints**:
- iOS enforces strict memory limits (typically 1-2 GB depending on device)
- The system may terminate the app if it exceeds the limit
- Use `os_proc_available_memory()` to check available memory before loading large documents

### Web (Wasm)

**Bundle size**:
- The Wasm binary and JavaScript glue code contribute to initial load time
- Use tree-shaking and dead code elimination (enabled by default in Kotlin/Wasm production builds)
- Consider lazy-loading format parsers that are not commonly used

**Service Worker caching**:
- The PWA service worker caches the Wasm binary and static assets for offline access
- Subsequent loads are fast because the cached Wasm binary is used

**Memory**:
- Web browsers allocate a fixed Wasm memory region (default: 256 MB, expandable)
- Very large documents may exceed the Wasm memory limit
- Monitor memory usage via the browser's developer tools (Memory tab)

**Network**:
- Web requests are subject to CORS restrictions
- WebDAV, FTP, and SMB protocols require a proxy server when accessed from the browser
- OAuth flows use popup windows or redirects

---

## Profiling and Measurement

### Benchmarks

Yole includes benchmarks in `shared/src/commonBenchmark/` and `shared/src/desktopBenchmark/`. Run them with:

```bash
# Inside Docker container
docker compose run --rm build ./gradlew :shared:desktopBenchmark
```

### Kover Coverage Reports

Coverage reports include execution counts that can highlight hot paths:

```bash
# Inside Docker container
docker compose run --rm build ./gradlew test koverHtmlReport
```

The HTML report is generated at `shared/build/reports/kover/html/index.html`.

### Logging

Enable verbose logging for network operations by setting the log level in your Ktor client configuration. This helps identify slow requests, retry storms, and authentication issues.

---

## Summary of Tuning Parameters

| Area | Parameter | Default | Recommended Range |
|------|-----------|---------|-------------------|
| Rate limiting | Token bucket capacity | Varies | 10-100 tokens |
| Rate limiting | Semaphore permits (RateLimiter) | 4 | 2-20 |
| Circuit breaker | Failure threshold | 5 | 3-10 |
| Circuit breaker | Reset timeout | 30s | 15-120s |
| Connection limiter | Max concurrent | 5 | 2-20 |
| Connection limiter | Acquire timeout | 30s | 10-120s |
| Network | Connection timeout | 30,000ms | 5,000-120,000ms |
| Network | Read timeout (WebDAV) | 60,000ms | 10,000-300,000ms |
| Cache | DocumentCache maxSize | 100 | 50-1000 |
| Cache | Entry priority | 100 | 1-1000 |
| Cache | StyleSheets cache | Unbounded | Clear on theme change |
| JVM | Heap size | Default | 512m-4g |
| Lazy init | Parser registration | Lazy | Lazy (recommended) |
| Lazy init | FormatRegistry formats | Lazy | Lazy (recommended) |

---

## Related Documentation

- [Architecture Guide](../ARCHITECTURE.md) -- System design overview
- [Build System Guide](BUILD_SYSTEM.md) -- Build commands and configuration
- [Troubleshooting](TROUBLESHOOTING.md) -- Common issues and solutions

---

*Last updated: March 17, 2026*
