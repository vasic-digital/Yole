# Module 17: Monitoring & Performance (7 videos)

## Video 17.1: Resilience Patterns Overview (15 min)

### Timestamps
- 0:00 Introduction: why resilience matters in an offline-first editor with cloud sync
- 2:00 The three pillars: circuit breakers, rate limiting, connection management
- 4:00 How Yole's extracted KMP modules provide resilience utilities
- 6:00 `RateLimiter-KMP`: token bucket, adaptive rate limiting, operation throttling
- 8:00 `Concurrency-KMP`: lazy loading, platform synchronization
- 10:00 Network service resilience: retry logic, timeout handling, error recovery
- 12:00 Resource lifecycle management: connect, use, disconnect, cleanup
- 12:30 `DocumentCache`: LRU cache with hit/miss tracking for `ParsedDocument` instances
- 13:00 Resilience testing: 53 dedicated tests in `ResilienceTests.kt`, 42 monitoring tests in `MonitoringMetricsTests.kt`
- 13:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/RateLimiting.kt` -- Facade bridging to RateLimiter-KMP
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/LazyLoading.kt` -- Facade bridging to Concurrency-KMP
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/NetworkStorageService.kt` -- Service interface with resilience patterns

### Exercises
1. **Map resilience patterns** -- Create a diagram showing which resilience pattern (circuit breaker, rate limiter, retry) is used by each network protocol service.

---

## Video 17.2: Rate Limiting Configuration (20 min)

### Timestamps
- 0:00 Why rate limiting: API quotas, fair usage, preventing lockout
- 2:00 Token bucket algorithm: capacity, refill rate, burst handling
- 4:00 The `TokenBucket` class: `tryAcquire()`, `acquire()` (blocking), token refill mechanics
- 6:00 `AdaptiveRateLimiter`: automatically adjusting rate based on 429 responses
- 8:00 `OperationThrottler`: queuing operations and executing within rate limits
- 10:00 Configuring rate limits per protocol: Dropbox (200 req/15min), Google Drive (1000 req/100s)
- 12:00 Rate limiter integration in service implementations
- 14:00 Testing rate limiting with `RateLimitedStorageServiceTest`
- 16:00 Backoff strategies: fixed, exponential, exponential with jitter
- 18:00 Monitoring rate limit consumption and headroom
- 19:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/RateLimiting.kt` -- `RateLimiter`, `TokenBucket`, `AdaptiveRateLimiter`, `OperationThrottler` typealiases
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/RateLimitedStorageServiceTest.kt` -- Rate limiting integration tests

### Key Code Walkthrough

The token bucket algorithm controls request flow:

```kotlin
// TokenBucket: fixed-rate limiting
val bucket = TokenBucket(
    capacity = 100,        // Maximum burst size
    refillRate = 10.0,     // Tokens per second
    refillInterval = 1000  // Refill check interval (ms)
)

// Non-blocking: returns false if no tokens available
if (bucket.tryAcquire()) {
    // Make API call
}

// AdaptiveRateLimiter: adjusts based on server responses
val adaptive = AdaptiveRateLimiter(
    initialRate = 50.0,
    minRate = 1.0,
    maxRate = 200.0
)
// On 429 response: rate automatically decreases
// On success: rate gradually increases back to max

// OperationThrottler: queue-based throttling
val throttler = OperationThrottler(rateLimiter = bucket)
throttler.submit {
    // This operation will wait for a token before executing
    dropboxService.listFiles("/")
}
```

### Exercises
1. **Configure rate limits** -- Set up a `TokenBucket` with Dropbox's rate limit (200 requests per 15 minutes) and write a test that verifies requests are throttled correctly.
2. **Adaptive test** -- Write a test that sends requests through an `AdaptiveRateLimiter`, simulates 429 responses, and verifies the rate decreases.
3. **Compare strategies** -- Implement fixed backoff, exponential backoff, and exponential backoff with jitter, then compare their behavior under sustained load.

---

## Video 17.3: Document Cache and Lazy Loading (18 min)

### Timestamps
- 0:00 Why caching: reducing parse latency, network calls, memory pressure
- 2:00 `ParsedDocument` HTML caching: lazy evaluation on first `toHtml()` call
- 4:00 `LazyDocumentLoader<T>`: defer loading until first access
- 6:00 `LazyStringLoader`: specialized loader for string content (file reading)
- 8:00 `FlowLazyLoader<T>`: Flow-based lazy loading with reactive updates
- 10:00 `DocumentCache` (LRU): eviction policy, configurable `maxSize`, hit/miss metrics via `hitRate` property
- 12:00 Cache invalidation strategies: content hash, timestamp, manual
- 14:00 Network document cache: caching downloaded files locally
- 16:00 Cache statistics: hit rate, eviction rate, memory consumption
- 17:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/LazyLoading.kt` -- `LazyDocumentLoader<T>`, `LazyStringLoader`, `FlowLazyLoader<T>` typealiases
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextParser.kt` -- `ParsedDocument` with lazy HTML caching
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/CacheEntry.kt` -- Network cache entry model
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/DocumentCache.kt` -- LRU cache with hit/miss tracking
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/CircuitBreaker.kt` -- Circuit breaker with state monitoring
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/ConnectionLimiter.kt` -- Semaphore-based connection limiting

### Key Code Walkthrough

`ParsedDocument` uses lazy caching for expensive HTML generation:

```kotlin
class ParsedDocument(
    val rawContent: String,
    val parsedContent: String,
    val metadata: Map<String, Any> = emptyMap(),
    val errors: List<ParseError> = emptyList(),
) {
    private var cachedHtml: String? = null

    fun toHtml(): String {
        // First call: generate and cache
        // Subsequent calls: return cached result
        return cachedHtml ?: generateHtml().also { cachedHtml = it }
    }
}
```

`LazyDocumentLoader` defers loading until first access:

```kotlin
val loader = LazyDocumentLoader<ParsedDocument> {
    // This lambda executes only when .value is first accessed
    parser.parse(readFileContent(path))
}

// Later, when needed:
val document = loader.value  // Triggers loading on first access
val html = document.toHtml() // Triggers HTML generation on first call
```

### Exercises
1. **Measure cache impact** -- Write a benchmark that parses a large Markdown file and calls `toHtml()` 1000 times. Compare the first call latency with subsequent calls.
2. **LRU eviction** -- Design an LRU cache for `ParsedDocument` objects with a maximum of 50 entries. Write tests that verify oldest entries are evicted when capacity is exceeded.

---

## Video 17.4: Connection Management (15 min)

### Timestamps
- 0:00 Connection lifecycle: connect, authenticate, operate, disconnect
- 2:00 Connection pooling for HTTP-based protocols (Ktor client)
- 4:00 `ConnectionLimiter`: semaphore-based connection limiting with `availablePermits` monitoring
- 6:00 Platform-specific connection constraints: mobile vs. desktop vs. web
- 8:00 Timeout configuration: connect timeout, read timeout, idle timeout
- 10:00 Reconnection strategies: immediate, delayed, exponential backoff
- 12:00 Health checking: detecting stale connections
- 13:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/NetworkStorageService.kt` -- `connect()`, `disconnect()`, `isOnline`
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/StorageConfig.kt` -- Timeout and connection configuration
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/NetworkPerformanceTest.kt` -- Performance tests

### Exercises
1. **Connection limiter** -- Implement a semaphore that limits concurrent connections to 5 and write a test where 20 coroutines attempt to connect simultaneously.
2. **Timeout tuning** -- Experiment with different timeout values in `StorageConfig` and measure the impact on connection reliability.

---

## Video 17.5: Performance Metrics and Testing (20 min)

### Timestamps
- 0:00 What to measure: parse time, render time, network latency, memory usage
- 2:00 Performance test infrastructure in Yole
- 4:00 `NetworkPerformanceTest`: measuring API call latency
- 6:00 `FormatParsingStressTest`: parser performance under load
- 8:00 `ResourceManagementStressTest`: resource lifecycle under pressure
- 10:00 Setting performance baselines and regression thresholds
- 12:00 Statistical analysis: mean, median, p95, p99, standard deviation
- 14:00 Automated performance regression detection in CI
- 16:00 Visualizing performance trends over time
- 18:00 Common performance pitfalls: string concatenation, regex recompilation, allocation pressure
- 19:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/NetworkPerformanceTest.kt` -- Network performance tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/FormatParsingStressTest.kt` -- Parsing stress tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/ResourceManagementStressTest.kt` -- Resource management stress tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/util/StressAndIntegrationTest.kt` -- Utility stress tests

### Key Concept: Performance Test Structure

```kotlin
@Test
fun `parse large markdown document under 100ms`() {
    val largeContent = generateMarkdownDocument(lines = 10_000)
    val parser = MarkdownParser()

    val startTime = System.currentTimeMillis()
    val result = parser.parse(largeContent)
    val elapsed = System.currentTimeMillis() - startTime

    assertThat(result.errors).isEmpty()
    assertThat(elapsed).isLessThan(100)
}
```

### Exercises
1. **Parser benchmark** -- Write a performance test that parses a 10,000-line document in each of the 17 formats and ranks them by parse time.
2. **Regression threshold** -- Establish a baseline for Markdown parsing of a 1,000-line document, then introduce a deliberate performance regression (e.g., recompile regex in a loop) and verify the test catches it.
3. **Memory profiling** -- Use JVM memory tools to measure allocation pressure during parsing of a 100,000-line file and identify the top allocation sites.

---

## Video 17.6: Memory Profiling (15 min)

### Timestamps
- 0:00 Memory profiling in KMP: JVM tools for desktop, Android Profiler for mobile
- 2:00 VisualVM for desktop JVM profiling: heap dumps, allocation tracking
- 4:00 Java Flight Recorder (JFR): low-overhead production profiling
- 6:00 Identifying memory hotspots: String allocations in parsers, HTML generation
- 8:00 Garbage collection analysis: GC pause times, promotion rates
- 10:00 Memory leak detection: retained objects, reference chains
- 12:00 Platform-specific constraints: Wasm memory limits, mobile low-memory handlers
- 13:30 Summary

### Exercises
1. **Heap dump analysis** -- Take a heap dump while parsing a large document and identify the top 5 object types by retained memory.
2. **GC tuning** -- Compare G1GC and ZGC pause times when parsing 100 documents concurrently.

---

## Video 17.7: Benchmark Tests (18 min)

### Timestamps
- 0:00 Benchmark source sets: `commonBenchmark` and `desktopBenchmark`
- 2:00 Writing repeatable benchmarks: warmup, iterations, statistical significance
- 4:00 Parser benchmarks: measuring throughput (documents per second)
- 6:00 Network benchmarks: measuring API call latency under load
- 8:00 UI benchmarks: measuring recomposition time
- 10:00 Benchmark harness: setup, warmup, measurement, teardown
- 12:00 Comparing benchmark results across commits
- 14:00 CI integration: tracking performance over time
- 16:00 Alerting on performance degradation: threshold-based alerts
- 17:30 Summary

### Key Code Walkthrough

Benchmark test structure:

```kotlin
// desktopBenchmark source set
class MarkdownParserBenchmark {
    private val parser = MarkdownParser()
    private val smallDoc = generateMarkdownDocument(100)
    private val mediumDoc = generateMarkdownDocument(1_000)
    private val largeDoc = generateMarkdownDocument(10_000)

    @Benchmark
    fun parseSmallDocument() = parser.parse(smallDoc)

    @Benchmark
    fun parseMediumDocument() = parser.parse(mediumDoc)

    @Benchmark
    fun parseLargeDocument() = parser.parse(largeDoc)

    @Benchmark
    fun htmlGenerationCached() {
        val doc = parser.parse(mediumDoc)
        repeat(100) { doc.toHtml() }  // Tests cache hit performance
    }
}
```

### Exercises
1. **Write a benchmark** -- Create a benchmark comparing parse throughput of Markdown, Todo.txt, and CSV for documents of identical line count.
2. **Benchmark comparison** -- Run the same benchmark on two different commits and calculate the percentage change in throughput.
3. **Cache efficiency** -- Benchmark `toHtml()` with and without the lazy cache to quantify the performance benefit.
