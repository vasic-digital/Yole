# Module 6: Performance Optimization (8 videos)

## Video 6.1: Performance Profiling (25 min)

### Timestamps
- 0:00 Introduction to performance profiling in KMP
- 2:00 Android: Android Studio Profiler overview
- 5:00 CPU profiling: identifying hotspots in parsing
- 8:00 Memory profiling: tracking allocations and GC pressure
- 11:00 Network profiling: monitoring API call latency
- 14:00 Desktop: JVM profiling with VisualVM
- 17:00 Java Flight Recorder (JFR) for production profiling
- 20:00 Identifying hotspots in parsing and rendering
- 23:00 Profiling best practices: measure first, optimize second
- 24:30 Summary

---

## Video 6.2: Parser Optimization (20 min)

### Timestamps
- 0:00 Current parser performance baseline
- 2:00 Lazy parsing: only parse visible lines
- 5:00 Incremental parsing: re-parse changed regions only
- 8:00 Caching strategy: HTML output cache with invalidation, `DocumentCache` LRU cache for parsed documents
- 11:00 Regex compilation: pre-compiling patterns
- 14:00 String allocation reduction: StringBuilder usage
- 17:00 Benchmarking before and after optimizations
- 19:30 Summary

---

## Video 6.3: UI Performance (18 min)

### Timestamps
- 0:00 Compose recomposition: understanding the lifecycle
- 2:00 Recomposition tracking and debugging
- 4:00 Reducing unnecessary recompositions with `remember`
- 6:00 `derivedStateOf` for computed values
- 8:00 `LazyColumn` for rendering large document lists
- 10:00 Key-based recomposition with stable keys
- 12:00 Side effects: `LaunchedEffect` and `SideEffect` usage
- 14:00 Frame timing and jank detection
- 16:00 Performance testing with Compose benchmarks
- 17:30 Summary

---

## Videos 6.4-6.8: Platform-Specific Optimization

### Video 6.4: Android Rendering Pipeline Optimization (15 min)

#### Timestamps
- 0:00 Android rendering pipeline overview
- 2:00 View vs. Compose rendering differences
- 4:00 GPU profiling with GPU rendering tools
- 6:00 Overdraw detection and reduction
- 8:00 Bitmap caching for preview rendering
- 10:00 RecyclerView vs. LazyColumn performance
- 12:00 Startup time optimization: app startup library
- 14:00 Summary

### Video 6.5: Desktop JVM Tuning (15 min)

#### Timestamps
- 0:00 JVM garbage collection options for desktop apps
- 2:00 Heap size tuning: -Xmx, -Xms
- 4:00 G1GC vs. ZGC for desktop applications
- 6:00 JIT compilation warmup and tiered compilation
- 8:00 Class loading optimization
- 10:00 Native image generation with GraalVM
- 12:00 Memory-mapped file I/O for large documents
- 14:00 Summary

### Video 6.6: Wasm Binary Size Reduction (15 min)

#### Timestamps
- 0:00 Wasm binary size challenges
- 2:00 Tree shaking and dead code elimination
- 4:00 Kotlin/Wasm optimization flags
- 6:00 Dependency analysis: removing unused libraries
- 8:00 Lazy loading of format parsers
- 10:00 Code splitting strategies
- 12:00 Compression: Brotli and gzip for deployment
- 14:00 Summary

### Video 6.7: Memory Management Across Platforms (15 min)

#### Timestamps
- 0:00 Memory model differences: JVM, Native, Wasm
- 2:00 Object lifecycle and finalization
- 4:00 Memory pressure handling on mobile
- 6:00 Large document strategies: streaming vs. loading
- 8:00 Cache eviction policies (LRU via `DocumentCache`, hit/miss tracking)
- 10:00 Weak references and soft references
- 12:00 Memory leak detection tooling
- 14:00 Summary

### Video 6.8: Benchmarking Framework and Regression Testing (15 min)

#### Timestamps
- 0:00 Benchmark source sets: commonBenchmark, desktopBenchmark
- 2:00 Writing repeatable benchmarks
- 4:00 Statistical analysis: mean, median, percentiles
- 6:00 CI integration: detecting performance regressions
- 8:00 Benchmark reporting and visualization
- 10:00 Alerting on performance degradation
- 12:00 Benchmark database and historical tracking
- 14:00 Summary
