<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Module 24: Performance Optimization (8 videos)

## Video 24.1: Performance Architecture Overview (15 min)

### Timestamps
- 0:00 Introduction: performance goals for a cross-platform text editor
- 2:00 The performance stack: lazy loading, caching, rate limiting, background processing
- 4:00 Key metrics: parse time <100ms, HTML cache hit <1ms, format detection <1ms
- 6:00 Performance test infrastructure: PerformanceMetricsTests and MonitoringMetricsTests
- 8:00 Profiling tools: Kover for hot paths, JFR for JVM, browser DevTools for Wasm
- 10:00 The 9,400+ test suite as a regression safety net
- 12:00 Platform-specific performance considerations (Android, Desktop, iOS, Wasm)
- 14:00 Summary

### Code References
- `docs/PERFORMANCE_TUNING.md`
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/PerformanceMetricsTests.kt`
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/MonitoringMetricsTests.kt`

---

## Video 24.2: FormatRegistry Lazy Initialization (18 min)

### Timestamps
- 0:00 The problem: 17 TextFormat entries created at startup
- 2:00 Solution: `lazy { createFormats() }` defers construction to first access
- 4:00 The isFormatsInitialized guard: checking without triggering
- 6:00 Benchmarks: 0ms startup (deferred) vs. 30-50ms (eager)
- 8:00 Thread safety: LazyThreadSafetyMode.SYNCHRONIZED under concurrent first-access
- 10:00 Parser lazy registration: factory lambdas in ParserInitializer
- 12:00 Measuring pending vs. instantiated parser count
- 14:00 Impact on cold start vs. warm start performance
- 16:00 Testing lazy initialization: MonitoringMetricsTests lazy loading section
- 17:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt`
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/ParserInitializer.kt`
- `docs/LAZY_LOADING.md`

---

## Video 24.3: ParsedDocument Lazy HTML Caching (15 min)

### Timestamps
- 0:00 The problem: HTML generation is expensive (5-10KB output, string concatenation)
- 2:00 Solution: lazy caching with separate light/dark mode caches
- 4:00 @Volatile for thread-safe cache variables
- 6:00 Memory savings: 50-70% for documents that are parsed but never previewed
- 8:00 clearHtmlCache() for explicit memory reclamation
- 10:00 hasHtmlCached() for monitoring and testing
- 12:00 Benchmark: first call vs. cached call (100x+ speedup)
- 14:00 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextParser.kt` -- ParsedDocument

---

## Video 24.4: DocumentCache LRU Strategy (18 min)

### Timestamps
- 0:00 DocumentCache design: bounded LRU with hit/miss tracking
- 2:00 Eviction criteria: expired -> lowest priority -> least recently accessed
- 4:00 Cache size tuning: mobile (50), desktop (100), server (1000+)
- 6:00 Hit rate monitoring and interpretation (>0.90 excellent, <0.50 poor)
- 8:00 FormatRegistry.parseWithCache() integration
- 10:00 Cooperative cancellation: yield() in long-running eviction loops
- 12:00 Pinned entries: preventing eviction of frequently accessed documents
- 14:00 Cache performance under concurrent access
- 16:00 Testing: ComprehensiveStressTests cache stress section
- 17:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/DocumentCache.kt`
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt` -- parseWithCache()

---

## Video 24.5: StyleSheets Caching and Theme Performance (12 min)

### Timestamps
- 0:00 StyleSheets.styleSheetCache: CSS generation caching
- 2:00 Cache key design: formatId + lightMode combination
- 4:00 clearCache() on theme changes
- 6:00 Impact: avoiding CSS regeneration on every document render
- 8:00 Light/dark mode switching performance
- 10:00 Custom CSS and cache invalidation
- 11:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/StyleSheets.kt`

---

## Video 24.6: Network Protocol Performance Tuning (18 min)

### Timestamps
- 0:00 HttpClient lazy initialization: deferring engine creation
- 2:00 Connection pooling: Ktor's internal pool reuses connections
- 4:00 Rate limiting: TokenBucket, AdaptiveRateLimiter, OperationThrottler
- 6:00 CircuitBreaker tuning: failure threshold, reset timeout
- 8:00 ConnectionLimiter tuning: max concurrent, acquire timeout
- 10:00 Timeout configuration: connection vs. read timeouts
- 12:00 Streaming for large file transfers: progressive data without buffering
- 14:00 Platform-specific tuning: Android battery, Desktop parallel ops, Wasm CORS
- 16:00 Performance testing: NetworkPerformanceTest baselines
- 17:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/CircuitBreaker.kt`
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/ConnectionLimiter.kt`
- `docs/PERFORMANCE_TUNING.md`

---

## Video 24.7: Parser-Specific Optimization (15 min)

### Timestamps
- 0:00 Parser complexity ranking: Markdown (high) to PlainText (minimal)
- 2:00 TodoTxtParser: the >10K character guard for regex backtracking prevention
- 4:00 Markdown: Flexmark with 16+ extensions, large document strategies
- 6:00 Jupyter: JSON deserialization, handling source as array vs. string
- 8:00 CSV: simple split-based parsing for fast performance
- 10:00 SftpService: trailing slash directory detection optimization
- 12:00 General patterns: avoiding regex compilation in hot loops, StringBuilder vs. concatenation
- 14:00 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/todotxt/TodoTxtParser.kt`
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/markdown/MarkdownParser.kt`

---

## Video 24.8: Performance Monitoring in CI (15 min)

### Timestamps
- 0:00 The CI performance safety net: assertion-based thresholds in tests
- 2:00 PerformanceMetricsTests: baselines for all critical operations
- 4:00 MonitoringMetricsTests: 42 tests across 6 categories
- 6:00 Detecting regressions: when a test fails, it blocks the merge
- 8:00 Kover coverage reports: identifying hot paths and optimization targets
- 10:00 Container-based benchmarks: consistent hardware via Docker
- 12:00 Interpreting results: parse time tables, HTML generation ratios
- 14:00 Summary

### Exercises
1. **Profile all 17 formats** -- Parse a 1000-line document in each format and rank by parse time.
2. **Optimize a slow parser** -- Identify the slowest format from Exercise 1 and improve its performance by 20%.
3. **Cache effectiveness study** -- Simulate a realistic editing session and measure DocumentCache hit rate over time.
