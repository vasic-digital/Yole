<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Module 29: Performance Optimization (8 videos)

## Learning Objectives

- Understand the PerformanceMetrics infrastructure and MetricsSnapshot data model
- Master tiered caching strategies: DocumentCache LRU, StyleSheets cache, ParsedDocument lazy HTML
- Learn platform-aware semaphore configuration for optimal concurrency on each target
- Implement configurable parse concurrency with dynamic tuning
- Apply async format detection for responsive document opening

---

## Video 29.1: Performance Architecture Overview (16 min)

### Timestamps
- 0:00 Introduction: why performance matters more in a cross-platform editor than a native one
- 2:00 The performance stack: parsing pipeline, caching layers, network protocols, UI rendering
- 4:00 Identifying bottlenecks: profiling on Android (systrace), Desktop (VisualVM), Wasm (browser DevTools)
- 6:00 Performance budgets: document open under 200ms, format detection under 10ms, HTML generation under 50ms
- 8:00 The PerformanceMetrics singleton: collecting timing data across all subsystems
- 10:00 MetricsSnapshot: immutable data class capturing a point-in-time view of all metrics
- 12:00 Metrics collection points: FormatRegistry, DocumentCache, StyleSheets, protocol services
- 14:00 Reporting: how metrics flow from collection to dashboard or test assertions
- 15:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt` -- Registry metrics
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/DocumentCache.kt` -- Cache hit/miss tracking
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/StyleSheets.kt` -- CSS cache metrics

---

## Video 29.2: Tiered Caching Strategy (18 min)

### Timestamps
- 0:00 The three caching tiers: document cache, HTML cache, stylesheet cache
- 2:00 Tier 1 -- DocumentCache: LRU eviction, configurable capacity, thread-safe access
- 4:00 Cache key design: file path + modification timestamp for cache invalidation
- 6:00 Hit/miss tracking: counters for monitoring cache effectiveness in production
- 8:00 Tier 2 -- ParsedDocument lazy HTML: @Volatile fields for light and dark HTML variants
- 10:00 Lazy HTML generation: first access triggers toHtml(), subsequent reads are instant
- 12:00 Tier 3 -- StyleSheets.styleSheetCache: generated CSS cached per theme configuration
- 14:00 Cache coherence: clearCache() methods and when to invalidate each tier
- 16:00 Memory pressure: sizing caches appropriately for each platform (Android < Desktop < Server)
- 17:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/DocumentCache.kt` -- LRU cache
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextParser.kt` -- Lazy HTML caching
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/StyleSheets.kt` -- CSS cache

---

## Video 29.3: Platform-Aware Semaphores (16 min)

### Timestamps
- 0:00 Why concurrency limits differ by platform: thread pools, memory, CPU cores
- 2:00 Android: limited memory, battery-conscious, fewer concurrent operations
- 4:00 Desktop: abundant memory and CPU, higher concurrency limits
- 6:00 Wasm: single-threaded, semaphore permits effectively serialize operations
- 8:00 iOS: main-actor constraints, background task limits
- 10:00 ConnectionLimiter: Semaphore-based concurrency cap, configurable per protocol
- 12:00 Default permit counts: how Yole chooses sensible defaults for each platform
- 14:00 Dynamic tuning: adjusting semaphore permits based on observed performance
- 15:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/ConnectionLimiter.kt`
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/RateLimiting.kt` -- Rate limiter facade

---

## Video 29.4: Configurable Parse Concurrency (16 min)

### Timestamps
- 0:00 The problem: parsing 10 documents simultaneously can starve other operations
- 2:00 Parse concurrency limit: a dedicated semaphore for parse operations
- 4:00 Configuration: setting max concurrent parses via app configuration
- 6:00 Batch operations: opening a folder with 50 documents -- controlled parallel parsing
- 8:00 Priority parsing: the foreground document gets priority over background pre-parsing
- 10:00 Backpressure: when parse requests exceed the limit, they suspend and queue
- 12:00 Monitoring: tracking parse queue depth and wait times in PerformanceMetrics
- 14:00 Testing: stress tests that verify correct behavior under parse saturation
- 15:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt` -- parseWithCache
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/DocumentCache.kt` -- Concurrent access
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/` -- Parse concurrency tests

---

## Video 29.5: Async Format Detection (18 min)

### Timestamps
- 0:00 Format detection overview: extension-based (fast) vs content-based (slower)
- 2:00 Extension-based detection: O(1) lookup via extension map, always tried first
- 4:00 Content-based detection: regex pattern matching against file content, costlier
- 6:00 Detection priority order: more specific formats before general ones in FormatRegistry
- 8:00 Async detection workflow: start extension detection, fall back to content detection off main thread
- 10:00 Progressive detection: show a "loading" state while content detection runs
- 12:00 Detection caching: remembering detected format for a file path to avoid re-detection
- 14:00 Binary detection: identifying non-text files early to avoid parsing attempts
- 16:00 Benchmarks: measuring detection time for all 17 text formats plus binary
- 17:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt` -- detect methods
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextFormat.kt` -- Detection patterns

---

## Video 29.6: Parser-Specific Optimization (16 min)

### Timestamps
- 0:00 Markdown: Flexmark with 16+ extensions -- which extensions cost the most
- 2:00 Flexmark optimization: reusing parser and renderer instances, avoiding re-initialization
- 4:00 Todo.txt: regex backtracking protection for inputs exceeding 10K characters
- 6:00 CSV/TSV: streaming parse for large spreadsheets, row-at-a-time processing
- 8:00 LaTeX: math expression caching to avoid re-rendering unchanged equations
- 10:00 Org Mode: hierarchical parsing with lazy child expansion
- 12:00 Binary detection: fast magic-byte check before attempting text parsing
- 14:00 Benchmarking individual parsers: comparative latency for a 10KB document per format
- 15:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/markdown/` -- Markdown parser
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/todotxt/` -- Todo.txt parser
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/csv/` -- CSV parser

---

## Video 29.7: Network Protocol Performance (16 min)

### Timestamps
- 0:00 Network latency: the dominant cost in cloud storage operations
- 2:00 Connection pooling: Ktor HttpClient connection reuse and keep-alive
- 4:00 Request pipelining: multiple requests on a single connection where protocols allow
- 6:00 Pagination: listing large directories with cursor-based pagination
- 8:00 Upload optimization: chunked uploads for large files (Dropbox, Google Drive)
- 10:00 Download optimization: range requests for partial file access
- 12:00 Offline queue performance: batching queued operations on reconnect
- 14:00 Protocol comparison benchmarks: latency profiles for all 8 services
- 15:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/` -- All 8 services
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/NetworkStorageService.kt`

---

## Video 29.8: Performance Testing and Monitoring (14 min)

### Timestamps
- 0:00 Performance baseline tests: establishing latency thresholds per operation
- 2:00 Regression detection: failing tests when performance degrades beyond threshold
- 4:00 Benchmark suites: running benchmarks on each platform and comparing
- 6:00 Memory profiling: tracking allocations during parsing and caching
- 8:00 GC pressure: minimizing object creation in hot paths
- 10:00 Kover coverage: using coverage data to identify untested performance-critical paths
- 12:00 Continuous monitoring: integrating performance metrics into the development workflow
- 13:30 Summary

### Exercises
1. **Cache sizing experiment**: Vary DocumentCache capacity from 10 to 1000 entries, measure hit rates with a realistic document access pattern, and graph the results.
2. **Parser benchmark**: Create a benchmark that measures parse time for each of the 17 text formats with a standardized 10KB input, rank them, and identify the slowest.
3. **Semaphore tuning**: Measure throughput of 100 concurrent network operations with semaphore permits set to 5, 10, 20, and 50. Find the optimal value for Desktop.
4. **Detection optimization**: Profile format detection for 100 files with known extensions and 100 files without extensions. Measure the cost of content-based detection fallback.
5. **Memory profiling**: Use a memory profiler to measure heap usage during a batch parse of 50 different-format documents. Identify the top 3 memory consumers.
