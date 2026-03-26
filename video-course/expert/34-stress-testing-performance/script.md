<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Module 34: Stress Testing & Performance Monitoring (8 videos)

## Prerequisites

- Module 11: Testing Strategies
- Module 21: Stress Testing & Responsiveness
- Module 17: Monitoring & Performance

## Learning Objectives

- Design per-protocol stress tests that surface real concurrency issues
- Test parser overload conditions without hitting platform memory limits
- Verify timeout recovery so protocol services degrade gracefully under load
- Use PerformanceMetrics, MetricsSnapshot, and MetricsReporter to track regressions
- Distinguish stress test failures caused by bugs vs resource constraints

---

## Video 34.1: Stress Testing Philosophy in KMP (12 min)

### Timestamps
- 0:00 Introduction: why stress testing is different from unit and integration testing
- 2:00 Goal 1: surface concurrency bugs that only appear under high contention
- 4:00 Goal 2: verify circuit breaker and rate limiter behaviour under saturation
- 6:00 Goal 3: establish performance baselines and detect regressions
- 8:00 KMP constraints: runBlocking<Unit> in JUnit4, no GlobalScope, coroutine scope discipline
- 10:00 Resource limits: how to avoid OOM kills in container environments (Exit 137)
- 11:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/` -- Stress test directory
- `docker-compose.yml` -- mem_limit configuration for container test runs

---

## Video 34.2: Per-Protocol Stress Tests (16 min)

### Timestamps
- 0:00 The structure: one stress test class per protocol service
- 2:00 FtpService stress: 50 concurrent upload coroutines, verify no data corruption
- 4:00 SftpService stress: directory listing under concurrent modification
- 6:00 WebDavService stress: simultaneous connect/disconnect cycles
- 8:00 SmbService stress: concurrent listFiles calls with connection pool exhaustion
- 10:00 GitService stress: concurrent pull/push with conflict detection
- 12:00 Cloud service stress (Dropbox/GoogleDrive/OneDrive): rate limiter saturation with MockEngine
- 14:00 Assertions: verifying CircuitBreaker trips and recovers correctly
- 15:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/` -- Protocol stress test files
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/CircuitBreaker.kt`

---

## Video 34.3: Parser Overload Testing (14 min)

### Timestamps
- 0:00 Why parsers need overload tests: unbounded input can cause quadratic backtracking
- 2:00 Case study: TodoTxtParser regex backtracking on inputs >10K characters
- 4:00 The fix: input length guard before expensive regex operations
- 6:00 Overload test structure: generate 1,000+ large documents and parse them concurrently
- 8:00 Testing all 17 parsers: sharing a base stress harness, parameterising per format
- 10:00 parseSemaphore in FormatRegistry: limiting concurrent parse operations
- 12:00 Measuring throughput: documents per second at various concurrency levels
- 13:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt` -- parseSemaphore
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/todotxt/TodoTxtParser.kt` -- Length guard

---

## Video 34.4: Timeout Recovery Testing (14 min)

### Timestamps
- 0:00 What timeout recovery means: a service that times out must return to a usable state
- 2:00 Using Ktor MockEngine to simulate network timeouts
- 4:00 Test scenario: connect times out; verify isConnected remains false, scope is clean
- 6:00 Test scenario: request times out mid-operation; verify partial state is rolled back
- 8:00 Test scenario: repeated timeouts trip the CircuitBreaker; verify OPEN state
- 10:00 Test scenario: CircuitBreaker resets after cooldown; verify service accepts new requests
- 12:00 HttpTimeout configuration: 10s connect, 30s request -- why these values
- 13:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/CircuitBreaker.kt`
- `shared/src/desktopTest/kotlin/digital/vasic/yole/network/` -- MockEngine timeout tests

---

## Video 34.5: PerformanceMetrics Infrastructure (14 min)

### Timestamps
- 0:00 Introduction: why ad-hoc println timing is not enough
- 2:00 PerformanceMetrics singleton: recording operation durations with nanosecond precision
- 4:00 MetricsSnapshot: @Serializable data class capturing a point-in-time metrics view
- 6:00 MetricsReporter: generating human-readable and JSON reports from a MetricsSnapshot
- 8:00 Integration: adding PerformanceMetrics.record() calls to critical paths in protocol services
- 10:00 Retention policy: ring buffer of last N measurements to bound memory usage
- 12:00 Resetting metrics between test runs to prevent cross-test contamination
- 13:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/PerformanceMetrics.kt`
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/MetricsSnapshot.kt`
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/MetricsReporter.kt`

---

## Video 34.6: Performance Baseline Tests and Regression Detection (14 min)

### Timestamps
- 0:00 The baseline approach: record expected latency thresholds for critical operations
- 2:00 Markdown parse latency: <5ms p50, <20ms p99 for typical documents
- 4:00 FormatRegistry detection latency: <1ms for extension-based detection
- 6:00 Protocol connect latency: <500ms p95 with a real (or Mock) server
- 8:00 Writing a performance test: measure N iterations, compute percentiles, assert thresholds
- 10:00 Regression detection: comparing current run against stored baseline in CI
- 12:00 Handling flakiness: warm-up iterations, outlier filtering, multiple runs
- 13:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/` -- Performance baseline tests
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/PerformanceMetrics.kt`

---

## Video 34.7: Memory Leak Detection in Long-Running Tests (12 min)

### Timestamps
- 0:00 Memory leaks in KMP: scope leaks, uncancelled coroutines, growing caches
- 2:00 Detection strategy: run 10,000 parse operations and monitor heap growth
- 4:00 DocumentCache LRU: verifying eviction prevents unbounded growth
- 6:00 StyleSheets cache: verifying clearCache() releases all references
- 8:00 Protocol service scope: verifying disconnect() cancels all coroutines and releases resources
- 10:00 Interpreting heap snapshots on JVM: using jmap and MAT
- 11:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/DocumentCache.kt`
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/StyleSheets.kt`
- `shared/src/commonTest/kotlin/digital/vasic/yole/` -- MemoryLeakDetectionTests

---

## Video 34.8: Integrating Stress Tests Into the Development Workflow (12 min)

### Timestamps
- 0:00 Which stress tests to run every commit vs weekly vs pre-release
- 2:00 Every commit: fast stress tests (<30s total) as part of :shared:desktopTest
- 4:00 Weekly: full per-protocol stress suite in a container with realistic load
- 6:00 Pre-release: memory leak detection suite and performance baseline comparison
- 8:00 Interpreting failures: distinguishing real bugs from resource contention in constrained CI
- 10:00 Documenting performance regressions: linking test failures to MetricsReporter output
- 11:30 Summary

### Exercises
1. **Parser overload test**: Write a stress test that parses 500 Markdown documents concurrently and asserts all results are non-null. Measure throughput.
2. **Timeout recovery**: Using MockEngine, simulate a connect timeout on FtpService and assert isConnected is false after the failure.
3. **CircuitBreaker stress**: Write a test that triggers 10 consecutive failures on a protocol service and verifies the CircuitBreaker enters OPEN state.
4. **PerformanceMetrics integration**: Add a PerformanceMetrics.record() call to FormatRegistry.detectByContent() and write a test that asserts p99 latency is below 5ms.
5. **Memory leak check**: Run DocumentCache with 10,000 parse-and-cache cycles. Assert heap growth is below 50MB after GC.
