<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Module 21: Stress Testing & Responsiveness (8 videos)

## Video 21.1: Stress Testing Philosophy and Patterns (18 min)

### Timestamps
- 0:00 Introduction: why Yole invests heavily in stress testing
- 2:00 The test pyramid at scale: 9,400+ test methods including dedicated stress suites
- 4:00 Stress testing vs. load testing vs. fuzz testing: definitions and when to use each
- 6:00 Yole's stress test directory structure: `format/stress/`, `network/stress/`, `util/StressAndIntegrationTest.kt`
- 8:00 Key stress test classes: `FormatParsingStressTest`, `ComprehensiveStressTests`, `ConcurrentFormatParsingStressTest`, `EdgeCaseStressTest`
- 10:00 The golden rule applied to stress tests: never disable, always fix root causes
- 12:00 Designing reproducible stress tests: fixed seeds, bounded inputs, deterministic concurrency
- 14:00 Avoiding flaky stress tests: time budgets, cooperative cancellation, `yield()` in hot loops
- 16:00 Container-based stress testing: consistent resources via `docker compose run`
- 17:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/FormatParsingStressTest.kt` -- Core parsing stress tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/ComprehensiveStressTests.kt` -- Comprehensive stress suite
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/ConcurrentFormatParsingStressTest.kt` -- Concurrent parsing
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/EdgeCaseStressTest.kt` -- Edge case stress tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/util/StressAndIntegrationTest.kt` -- Rate limiter, lazy loading, parser registry stress tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/model/DocumentStressTest.kt` -- Document model stress tests

### Exercises
1. **Classify existing stress tests** -- Read through all files in `format/stress/` and `network/stress/` and categorize each test by what it stresses: CPU, memory, concurrency, or I/O.
2. **Identify input bounds** -- For each stress test, find the maximum input size used and explain why that bound was chosen (hint: see the TodoTxtParser >10K guard).

---

## Video 21.2: Concurrent Format Parsing Stress Tests (20 min)

### Timestamps
- 0:00 The concurrency challenge: 17 parsers accessed simultaneously
- 2:00 `ConcurrentFormatParsingStressTest`: parsing the same document from 100+ coroutines
- 4:00 FormatRegistry thread safety: lazy initialization with `lazy { createFormats() }`
- 6:00 Testing parser statelessness: verify no shared mutable state across parse calls
- 8:00 Concurrent HTML generation: `ParsedDocument.toHtml()` lazy cache under contention
- 10:00 The `isFormatsInitialized` guard and why it matters for concurrent first-access
- 12:00 `ParserRegistryStressTest`: concurrent detection + parsing + registration
- 14:00 Measuring contention: timing parallel vs. sequential parse to identify lock overhead
- 16:00 Regression: the TodoTxtParser regex backtracking fix and the >10K input guard
- 18:00 Writing your own concurrent parsing stress test
- 19:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/ConcurrentFormatParsingStressTest.kt` -- Primary concurrent parsing tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/util/StressAndIntegrationTest.kt` -- `ParserRegistryStressTest` class
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt` -- Lazy format list with `isFormatsInitialized`
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextParser.kt` -- `ParsedDocument` with lazy HTML caching
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/todotxt/TodoTxtParser.kt` -- >10K input guard for regex safety

### Key Concept: Concurrent Parser Safety

```kotlin
// Stress test: 100 coroutines parsing simultaneously
@Test
fun concurrentParsingShouldNotCorruptResults() = runBlocking<Unit> {
    val document = generateLargeMarkdownDocument(lines = 500)
    val results = (1..100).map {
        async(Dispatchers.Default) {
            FormatRegistry.detectAndParse(document)
        }
    }.awaitAll()

    // All 100 results must be identical
    val reference = results.first()
    results.forEach { result ->
        assertEquals(reference.parsedContent, result.parsedContent)
        assertEquals(reference.metadata, result.metadata)
    }
}
```

### Exercises
1. **Write a cross-format concurrent test** -- Create a stress test where 10 different formats are parsed simultaneously by 10 coroutines each (100 total) and verify no cross-contamination occurs.
2. **Measure lock contention** -- Time `FormatRegistry.detectByExtension()` under 1, 10, 50, and 100 concurrent callers. Plot the results and identify the contention knee.

---

## Video 21.3: Protocol Overload and Circuit Breaker Testing (20 min)

### Timestamps
- 0:00 Network protocols under stress: what happens when the server is slow or down
- 2:00 `ProtocolOverloadStressTest`: flooding all 8 protocol services with concurrent requests
- 4:00 CircuitBreaker behavior under load: CLOSED -> OPEN transition threshold
- 6:00 Testing the HALF_OPEN state: allowing probe requests after reset timeout
- 8:00 `NetworkProtocolStressTest`: sustained high-frequency operations
- 10:00 `ConcurrencyStressTest`: concurrent connect/disconnect/reconnect cycles
- 12:00 ConnectionLimiter saturation: what happens when all semaphore permits are taken
- 14:00 `ResourceManagementStressTest`: verifying no connection leaks under load
- 16:00 `DatabaseStressTest`: concurrent metadata reads/writes during protocol operations
- 18:00 Combining circuit breaker + rate limiter + connection limiter: the full resilience stack
- 19:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/ProtocolOverloadStressTest.kt` -- Protocol overload tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/NetworkProtocolStressTest.kt` -- Network protocol stress tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/ConcurrencyStressTest.kt` -- Connection lifecycle stress tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/ResourceManagementStressTest.kt` -- Resource leak detection
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/DatabaseStressTest.kt` -- Database stress tests
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/CircuitBreaker.kt` -- CircuitBreaker implementation
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/ConnectionLimiter.kt` -- ConnectionLimiter implementation

### Exercises
1. **Trip the circuit breaker** -- Write a test that makes requests until the CircuitBreaker opens, then verify all subsequent requests fail fast without hitting the server. Wait for the reset timeout and verify the HALF_OPEN probe succeeds.
2. **Saturate the connection limiter** -- Create a test with more concurrent requests than the ConnectionLimiter allows and verify excess requests are suspended (not rejected) until permits become available.

---

## Video 21.4: Cache Overflow and LRU Stress Testing (18 min)

### Timestamps
- 0:00 DocumentCache: LRU eviction under memory pressure
- 2:00 Cache configuration: maximum size, eviction policy, hit/miss tracking
- 4:00 Stress test: inserting 10x the cache capacity and verifying LRU eviction order
- 6:00 Concurrent cache access: multiple coroutines reading and writing simultaneously
- 8:00 Cache invalidation under stress: clearing while reads are in progress
- 10:00 Hit/miss ratio tracking: measuring cache effectiveness under realistic workloads
- 12:00 The `clearCache()` mechanism in `StyleSheets.kt`: theme switch cache invalidation
- 14:00 Memory overhead measurement: cache size vs. actual memory consumption
- 16:00 Cooperative cancellation in DocumentCache: the `yield()` fix for long-running cache operations
- 17:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/DocumentCache.kt` -- LRU cache implementation
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt` -- `parseWithCache()` integration
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/StyleSheets.kt` -- `styleSheetCache` with `clearCache()`
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/ComprehensiveStressTests.kt` -- Cache stress tests

### Key Concept: LRU Eviction Under Load

```kotlin
@Test
fun cacheEvictsLeastRecentlyUsedUnderOverflow() = runBlocking<Unit> {
    val cache = DocumentCache(maxSize = 10)

    // Fill cache to capacity
    (1..10).forEach { i ->
        cache.put("doc-$i", parsedDocument("content-$i"))
    }

    // Access doc-1 to make it recently used
    cache.get("doc-1")

    // Overflow: insert 5 more documents
    (11..15).forEach { i ->
        cache.put("doc-$i", parsedDocument("content-$i"))
    }

    // doc-1 should survive (recently accessed), doc-2 through doc-6 should be evicted
    assertNotNull(cache.get("doc-1"))
    assertNull(cache.get("doc-2"))
}
```

### Exercises
1. **Measure cache hit ratio** -- Write a stress test that simulates a realistic editing session (80% reads of recent documents, 20% new documents) and measure the cache hit ratio. Aim for >70%.
2. **Concurrent cache stress** -- Create 50 coroutines that each insert and read 100 documents from the same cache instance. Verify no entries are corrupted.

---

## Video 21.5: Rate Limiter Saturation Testing (18 min)

### Timestamps
- 0:00 Rate limiting in Yole: protecting cloud API quotas
- 2:00 `RateLimiterStressTest`: bursting requests beyond the token bucket capacity
- 4:00 TokenBucket algorithm under sustained load: refill rate vs. consumption rate
- 6:00 `AdaptiveRateLimiter`: dynamic rate adjustment based on server response codes
- 8:00 `OperationThrottler`: per-operation type throttling (list, upload, download, delete)
- 10:00 Testing saturation: what happens when every token is consumed
- 12:00 Fairness under contention: verifying no coroutine starves when rate limited
- 14:00 Integration with protocol services: `RateLimitedStorageService` wrapper pattern
- 16:00 Measuring throttle overhead: added latency from rate limiting under normal load
- 17:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/util/StressAndIntegrationTest.kt` -- `RateLimiterStressTest` class
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/RateLimiting.kt` -- Facade bridges for rate limiting types
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/ProtocolOverloadStressTest.kt` -- Protocol-level rate limiting tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/NetworkProtocolStressTest.kt` -- Network rate limiting stress

### Exercises
1. **Token bucket exhaustion** -- Write a test that acquires tokens faster than the refill rate and measures the delay imposed on callers when the bucket is empty. Graph the latency over time.
2. **Adaptive rate test** -- Simulate a server returning HTTP 429 (Too Many Requests) and verify the `AdaptiveRateLimiter` reduces its rate. Then simulate recovery and verify the rate increases.

---

## Video 21.6: End-to-End Responsiveness Validation (20 min)

### Timestamps
- 0:00 Responsiveness goal: <100ms p99 for core operations
- 2:00 What counts as a core operation: format detection, parsing, HTML generation, cache lookup
- 4:00 `EndToEndFormatTests.kt`: full pipeline timing from raw input to rendered HTML
- 6:00 Measuring p50, p95, p99 latencies across all 17 formats
- 8:00 Large document responsiveness: 1,000-line and 10,000-line benchmarks
- 10:00 Responsiveness under concurrent load: parsing while other operations run
- 12:00 Cold start vs. warm start: first parse (FormatRegistry initialization) vs. subsequent parses
- 14:00 The lazy initialization payoff: FormatRegistry lazy loading eliminates cold-start penalty
- 16:00 Performance regression detection: CI baselines and alerting thresholds
- 18:00 Practical responsiveness testing: integrating timing assertions into existing tests
- 19:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/e2e/EndToEndFormatTests.kt` -- End-to-end format pipeline tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/load/FormatLoadTests.kt` -- Format load tests
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt` -- Lazy format initialization
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/NetworkPerformanceTest.kt` -- Network performance baselines

### Key Concept: p99 Latency Assertion

```kotlin
@Test
fun markdownParsingP99Under100ms() = runBlocking<Unit> {
    val document = generateMarkdownDocument(lines = 1000)
    val latencies = (1..100).map {
        measureTimeMillis {
            MarkdownParser().parse(document)
        }
    }.sorted()

    val p99 = latencies[98] // 99th percentile
    assertTrue(p99 < 100, "p99 latency was ${p99}ms, expected <100ms")
}
```

### Exercises
1. **Profile all 17 formats** -- Write a test that parses a standardized 500-line document in each format, collects p50/p95/p99 latencies, and prints a comparison table. Identify the slowest format.
2. **Cold start measurement** -- Measure the first-ever call to `FormatRegistry.detectByExtension()` vs. subsequent calls. Quantify the lazy initialization overhead.

---

## Video 21.7: Non-Blocking Guarantee Verification (18 min)

### Timestamps
- 0:00 Why non-blocking matters: the main thread must never be blocked
- 2:00 `NonBlockingOperationTests.kt`: the approach to verifying non-blocking behavior
- 4:00 Testing with `withTimeout`: if an operation blocks, the timeout fires
- 6:00 Verifying suspend functions actually suspend: `yield()` interleaving checks
- 8:00 Common blocking pitfalls in KMP: `runBlocking` inside suspend functions, synchronized blocks
- 10:00 The CancellationException rethrow rule: all catch blocks must rethrow `CancellationException`
- 12:00 Testing cancellation propagation: cancel a parent scope and verify child operations clean up
- 14:00 `serviceScope` lifecycle: cancellation on reconnect/disconnect to prevent coroutine leaks
- 16:00 Verifying non-blocking across all 8 protocol services
- 17:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/nonblocking/NonBlockingOperationTests.kt` -- Non-blocking verification tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/concurrency/ConcurrencySafetyTest.kt` -- Concurrency safety tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/util/ConcurrencySafetyTest.kt` -- Utility concurrency tests

### Key Concept: Verifying Non-Blocking with Timeout

```kotlin
@Test
fun formatDetectionNeverBlocks() = runBlocking<Unit> {
    // If detectByExtension blocks the coroutine, withTimeout will throw
    val result = withTimeout(50) {
        FormatRegistry.detectByExtension("test.md")
    }
    assertNotNull(result)
    assertEquals(TextFormat.ID_MARKDOWN, result.id)
}
```

### Exercises
1. **Audit CancellationException handling** -- Search all catch blocks in the 8 protocol services and verify each one rethrows `CancellationException`. Document any that do not.
2. **Write a cancellation test** -- Start a long-running protocol operation, cancel it after 10ms, and verify the operation terminates promptly and all resources (connections, file handles) are released.

---

## Video 21.8: Memory Leak Regression Testing Patterns (18 min)

### Timestamps
- 0:00 Memory leaks in long-running editor sessions: the silent performance killer
- 2:00 Common leak sources: uncancelled coroutine scopes, growing caches, event listener accumulation
- 4:00 The `serviceScope` fix: cancelling on reconnect/disconnect to prevent coroutine leaks
- 6:00 DocumentCache bounded size: LRU eviction prevents unbounded growth
- 8:00 StyleSheets `clearCache()`: preventing stale theme cache accumulation
- 10:00 Testing for leaks: repeated create/destroy cycles with memory measurement
- 12:00 Weak reference patterns: verifying objects are garbage collected
- 14:00 `ResourceManagementStressTest`: verifying connection pool cleanup under load
- 16:00 Regression test pattern: capture baseline memory, run operation N times, verify memory stays bounded
- 17:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/ResourceManagementStressTest.kt` -- Resource management and leak detection
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/ConcurrencyStressTest.kt` -- Scope lifecycle tests
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/DocumentCache.kt` -- Bounded LRU cache
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/StyleSheets.kt` -- `clearCache()` for theme switches

### Key Concept: Memory Regression Test Pattern

```kotlin
@Test
fun repeatedParsingDoesNotLeakMemory() = runBlocking<Unit> {
    val cache = DocumentCache(maxSize = 50)
    val runtime = Runtime.getRuntime()

    // Warm up and baseline
    repeat(100) { cache.put("warmup-$it", parsedDocument("warmup")) }
    cache.clear()
    System.gc()
    val baselineMemory = runtime.totalMemory() - runtime.freeMemory()

    // Run 1000 parse-and-cache cycles
    repeat(1000) { i ->
        cache.put("doc-$i", parsedDocument("content-$i"))
    }

    System.gc()
    val finalMemory = runtime.totalMemory() - runtime.freeMemory()
    val growth = finalMemory - baselineMemory

    // Cache holds at most 50 items, so memory should be bounded
    // Allow 2x baseline for overhead
    assertTrue(growth < baselineMemory * 2,
        "Memory grew by ${growth / 1024}KB, suspect leak")
}
```

### Exercises
1. **Connection leak test** -- Write a stress test that connects and disconnects from a mock protocol service 500 times and verifies no connection objects remain in memory after GC.
2. **Cache memory bound** -- Measure the actual memory consumption of a `DocumentCache` with maxSize=100 filled with documents of varying sizes (1KB, 10KB, 100KB). Verify the total stays below a reasonable threshold.
3. **Coroutine leak detection** -- Create a test that launches 100 coroutines within a `serviceScope`, cancels the scope, and verifies all coroutines have completed (using `Job.children.count()`).
