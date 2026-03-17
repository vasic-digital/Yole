# Monitoring Guide

This guide covers how to monitor Yole's runtime behavior, interpret performance metrics, and run benchmarks. Monitoring spans three areas: format parsing performance, network protocol resilience, and resource utilization.

## Performance Metrics Test Baselines

**Location:** `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/PerformanceMetricsTests.kt`

The performance metrics tests establish timing baselines for critical operations. These tests run on every CI build to detect performance regressions.

### Baseline Categories

| Category | What Is Measured | Typical Baseline |
|----------|-----------------|-----------------|
| Parse time | Time to parse a document of known size | < 100ms for 10,000 lines |
| HTML generation | Time for first `toHtml()` call | < 50ms for medium documents |
| HTML cache hit | Time for subsequent `toHtml()` calls | < 1ms (returns cached) |
| Format detection | Time for `FormatRegistry.detectByExtension()` | < 1ms |
| Content detection | Time for `FormatRegistry.detectByContent()` | < 5ms |
| Parser registration | Time for `registerAllParsersLazy()` | < 2ms |

### Running Performance Tests

```bash
# In container (mandatory)
docker compose run --rm build ./gradlew test \
  --tests "digital.vasic.yole.format.stress.PerformanceMetricsTests"

# Desktop only (faster iteration)
docker compose run --rm build ./gradlew :shared:desktopTest \
  --tests "digital.vasic.yole.format.stress.PerformanceMetricsTests"
```

---

## Monitoring Metrics

**Location:** `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/MonitoringMetricsTests.kt`

The monitoring metrics tests measure 6 categories with 42 total test methods:

### 1. Parse Time Monitoring

Measures the time to parse documents across all 17 formats. Ensures no single format degrades beyond acceptable thresholds.

### 2. HTML Generation Monitoring

Tracks the cost of converting `ParsedDocument` to HTML, including:
- First-call generation time
- Cached call return time
- Light vs. dark mode generation overhead

### 3. Detection Monitoring

Monitors format detection performance:
- Extension-based detection (should be < 1ms)
- Content-based detection (involves regex matching)
- Ambiguous extension handling (e.g., `.txt` could be plaintext or todo.txt)

### 4. Memory Monitoring

Tracks allocation patterns during parsing:
- Object creation count
- String allocation pressure
- GC pause impact

### 5. Throughput Monitoring

Measures documents-per-second parsing throughput:
- Sequential parsing throughput
- Concurrent parsing throughput
- Throughput under memory pressure

### 6. Lazy Loading Monitoring

Verifies lazy loading effectiveness:
- Parser instantiation timing (lazy vs. eager)
- Pending parser count after startup
- Instantiated parser count after usage

---

## DocumentCache Monitoring

The `DocumentCache` class exposes metrics through properties:

```kotlin
val cache = DocumentCache(maxSize = 100)

// After usage:
println("Cache size: ${cache.size}")       // Current entries
println("Hits: ${cache.hits}")             // Total cache hits
println("Misses: ${cache.misses}")         // Total cache misses
println("Hit rate: ${cache.hitRate}")       // Ratio (0.0 to 1.0)
```

### Interpreting Hit Rate

| Hit Rate | Interpretation | Action |
|----------|---------------|--------|
| > 0.90 | Excellent. Most documents are served from cache. | None needed |
| 0.70 - 0.90 | Good. Moderate cache effectiveness. | Consider increasing `maxSize` |
| 0.50 - 0.70 | Fair. Many cache misses. | Increase `maxSize` or review access patterns |
| < 0.50 | Poor. Cache is not effective. | Working set exceeds cache size; increase `maxSize` significantly |

### Recommended Cache Sizes

| Use Case | Recommended `maxSize` |
|----------|----------------------|
| Mobile (constrained memory) | 50 |
| Desktop (general use) | 100 (default) |
| Desktop (power user, many files open) | 200-500 |
| Server/batch processing | 1000+ |

---

## CircuitBreaker State Monitoring

Each protocol service's circuit breaker exposes state through properties:

```kotlin
val breaker = CircuitBreaker(name = "dropbox", failureThreshold = 5)

println("State: ${breaker.state}")         // CLOSED, OPEN, or HALF_OPEN
println("Failures: ${breaker.failures}")   // Consecutive failure count
println("Successes: ${breaker.successes}") // Total successes
println("Total calls: ${breaker.calls}")   // Total operations attempted
```

### State Interpretation

| State | Meaning | User Impact |
|-------|---------|-------------|
| CLOSED | Normal operation | None -- all operations proceed normally |
| OPEN | Service is failing; operations blocked | Operations fail immediately with `CircuitBreakerOpenException` |
| HALF_OPEN | Testing recovery after timeout | One trial operation permitted; success restores service |

### Failure Rate Calculation

```
failure_rate = failures / calls (over a time window)
```

A sustained failure rate above 20% suggests a systemic issue (network outage, revoked credentials, API changes).

---

## ConnectionLimiter Utilization

```kotlin
val limiter = ConnectionLimiter(name = "dropbox", maxConcurrent = 5)

println("Available: ${limiter.availablePermits}")  // Free slots
println("In use: ${limiter.maxConcurrent - limiter.availablePermits}")  // Active ops
```

### Utilization Interpretation

| Available Permits | Interpretation | Action |
|-------------------|---------------|--------|
| maxConcurrent | No active operations | Normal idle state |
| 1-2 | Moderate load | Normal operation |
| 0 | At capacity; new operations will suspend | Consider increasing `maxConcurrent` if operations queue frequently |

---

## Running Performance Benchmarks

### Benchmark Source Sets

Yole provides benchmark source sets for performance measurement:

```
shared/src/
├── commonBenchmark/    # Cross-platform benchmarks
└── desktopBenchmark/   # JVM-specific benchmarks
```

### Running Benchmarks

```bash
# Desktop benchmarks (JVM)
docker compose run --rm build ./gradlew :shared:desktopBenchmark

# All stress tests (acts as benchmarks)
docker compose run --rm build ./gradlew test \
  --tests "digital.vasic.yole.format.stress.*"
```

### Stress Test Suites

| Test Suite | Tests | Focus |
|------------|-------|-------|
| `FormatParsingStressTest` | Parsing under load | All 17 formats with large inputs |
| `ComprehensiveStressTests` | 30 tests | Concurrent parsing, cache contention, memory stability |
| `ResourceManagementStressTest` | Resource lifecycle | Connection management under pressure |
| `EdgeCaseStressTest` | Edge cases under load | Boundary conditions with concurrent access |

---

## Interpreting Results

### Parse Time Results

```
Format          | Lines | Parse Time | Status
----------------|-------|------------|-------
Markdown        | 10000 | 45ms       | PASS (< 100ms)
Todo.txt        | 10000 | 12ms       | PASS (< 100ms)
CSV             | 10000 | 8ms        | PASS (< 100ms)
LaTeX           | 10000 | 38ms       | PASS (< 100ms)
```

If a format consistently exceeds its baseline by more than 50%, investigate:
1. Recent parser changes
2. Regex pattern changes (recompilation in hot loop)
3. String allocation changes (StringBuilder vs. concatenation)

### HTML Generation Results

```
Scenario                | Time   | Status
------------------------|--------|-------
First toHtml() call     | 23ms   | PASS (< 50ms)
Cached toHtml() call    | 0.1ms  | PASS (< 1ms)
clearHtmlCache() + call | 24ms   | PASS (regeneration)
```

The ratio between first-call and cached-call times quantifies the cache effectiveness. A 100x+ speedup on cached calls is normal.

### Network Resilience Results

```
Pattern            | Metric              | Value  | Status
-------------------|---------------------|--------|-------
CircuitBreaker     | Opens after 5 fails | Yes    | PASS
CircuitBreaker     | Resets after timeout | Yes    | PASS
ConnectionLimiter  | Blocks at capacity  | Yes    | PASS
ConnectionLimiter  | Releases on complete| Yes    | PASS
RateLimiter        | Throttles correctly | Yes    | PASS
```

---

## Continuous Monitoring in CI

All monitoring tests run automatically on every push and pull request through the CI pipeline:

```bash
# Full test suite (includes all monitoring tests)
docker compose run --rm build ./docker/scripts/test-all.sh
```

The CI pipeline reports:
- Total test count (9,400+ methods across ~215 test files)
- Failure details with stack traces
- Coverage report via Kover (63.0% line coverage)

### Metrics Test Patterns

The monitoring tests follow a consistent pattern for measuring and asserting performance:

```kotlin
// Pattern: measure, collect, assert threshold
@Test
fun parseTimeUnderThreshold() = runBlocking<Unit> {
    val times = (1..50).map {
        measureTimeMillis { parser.parse(document) }
    }
    val p99 = times.sorted()[47] // 95th percentile of 50 samples
    assertTrue(p99 < 100, "p99 parse time ${p99}ms exceeds 100ms threshold")
}

// Pattern: cache effectiveness measurement
@Test
fun cacheHitRateAboveThreshold() = runBlocking<Unit> {
    val cache = DocumentCache(maxSize = 50)
    // Simulate realistic access pattern
    repeat(100) { i ->
        val key = "doc-${i % 20}" // 20 unique docs, 80% re-access rate
        cache.getOrPut(key) { parser.parse("content-$i") }
    }
    assertTrue(cache.hitRate > 0.70, "Cache hit rate ${cache.hitRate} below 0.70")
}
```

These patterns are used in `MonitoringMetricsTests.kt` and `PerformanceMetricsTests.kt` to establish baselines that detect regressions automatically in CI.

### Alerting on Regressions

Performance regressions are detected by assertion-based thresholds in the test suite. If a parse time exceeds its baseline threshold, the test fails and blocks the merge.

---

## Related Documentation

- [RESILIENCE_PATTERNS.md](RESILIENCE_PATTERNS.md) -- Detailed resilience pattern documentation
- [PERFORMANCE_TUNING.md](PERFORMANCE_TUNING.md) -- Performance tuning guide
- [LAZY_LOADING.md](LAZY_LOADING.md) -- Lazy loading patterns
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) -- Troubleshooting common issues
