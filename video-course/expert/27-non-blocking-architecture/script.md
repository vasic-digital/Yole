<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Module 27: Non-Blocking Architecture (8 videos)

## Learning Objectives

- Understand why non-blocking design is essential for cross-platform text editors
- Master Dispatchers.IO, Dispatchers.Default, and Dispatchers.Main usage in Yole
- Learn the lazy initialization cascade pattern and its thread-safety guarantees
- Understand Wasm single-thread constraints and how platformSynchronized bridges the gap
- Apply suspend function design patterns for responsive UIs on all four platforms

---

## Video 27.1: Why Non-Blocking Matters (14 min)

### Timestamps
- 0:00 Introduction: the cost of blocking in a cross-platform text editor
- 2:00 What "blocking" means at the OS level: thread stalls, ANR on Android, frozen UI on Desktop
- 4:00 Yole's architecture promise: every user-facing operation completes without blocking the main thread
- 6:00 The four platform constraints: Android main thread, Desktop EDT, Wasm single thread, iOS main actor
- 8:00 How Kotlin coroutines solve blocking without callback hell
- 10:00 Measuring blocking: detecting main-thread stalls with StrictMode (Android) and coroutine debugger
- 12:00 The 10,000+ test suite includes dedicated non-blocking verification tests
- 13:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt` -- Lazy format loading
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextParser.kt` -- Suspend parse functions
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/` -- Non-blocking stress tests

---

## Video 27.2: Dispatcher Strategy (16 min)

### Timestamps
- 0:00 The three dispatchers: IO, Default, and Main
- 2:00 Dispatchers.IO: file reads, network calls, all 8 protocol services
- 4:00 Dispatchers.Default: CPU-intensive parsing, format detection, HTML generation
- 6:00 Dispatchers.Main: UI updates, state emissions, Compose recomposition triggers
- 8:00 withContext() switching: when and why to switch dispatchers mid-coroutine
- 10:00 Platform-specific dispatcher mapping: how Wasm maps IO to Default (single-threaded)
- 12:00 CoroutineScope design: serviceScope in protocol services, viewModelScope in UI
- 14:00 SupervisorJob: why every service scope uses SupervisorJob for fault isolation
- 15:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/` -- Service scopes
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/CircuitBreaker.kt` -- IO dispatcher usage
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/PlatformSync.kt` -- Platform dispatcher bridge

---

## Video 27.3: Suspend Functions Design (18 min)

### Timestamps
- 0:00 Anatomy of a suspend function: suspension points, continuations, state machines
- 2:00 Rule 1: never block inside a suspend function -- use withContext(Dispatchers.IO) for blocking APIs
- 4:00 Rule 2: always rethrow CancellationException -- Yole's catch-block convention
- 6:00 Rule 3: check isActive or yield() in long loops for cooperative cancellation
- 8:00 DocumentCache.getOrParse(): suspend function with yield() for cancellation points
- 10:00 Format detection pipeline: detectByExtension() then detectByContent() with early returns
- 12:00 ParsedDocument.toHtml(): lazy caching with @Volatile for thread-safe first-call generation
- 14:00 Network protocol operations: suspend functions with Mutex and withLock patterns
- 16:00 Error handling: Result types in suspend functions, exception propagation to UI
- 17:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/DocumentCache.kt` -- yield() cancellation
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextParser.kt` -- Lazy HTML caching
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/DropboxService.kt` -- Suspend operations

---

## Video 27.4: Lazy Initialization Cascade (16 min)

### Timestamps
- 0:00 The lazy initialization pattern: deferring expensive setup until first use
- 2:00 FormatRegistry.formats: lazy { createFormats() } with isFormatsInitialized guard
- 4:00 StyleSheets.styleSheetCache: caching generated CSS to avoid recomputation
- 6:00 HttpClient lazy initialization: by lazy { } with @Volatile _httpClientAccessed flag
- 8:00 OAuth2Flow lazy delegates: deferring auth setup until user triggers cloud connection
- 10:00 The cascade effect: first document open triggers format detection, parsing, HTML gen, CSS gen
- 12:00 Measuring cascade cost: benchmarks showing first-open vs subsequent-open latency
- 14:00 Thread safety of lazy: LazyThreadSafetyMode.SYNCHRONIZED vs PUBLICATION vs NONE
- 15:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt` -- lazy formats
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/StyleSheets.kt` -- styleSheetCache
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/auth/` -- OAuth2Flow lazy init

---

## Video 27.5: Wasm Single-Thread Safety (18 min)

### Timestamps
- 0:00 The Wasm constraint: JavaScript runs on a single thread, no real parallelism
- 2:00 How kotlinx.coroutines handles Wasm: cooperative scheduling on the event loop
- 4:00 Why Mutex still matters on Wasm: coroutine interleaving can corrupt shared state
- 6:00 platformSynchronized: the expect/actual bridge for platform-specific locking
- 8:00 Desktop/Android: platformSynchronized maps to synchronized(lock) {} (real thread safety)
- 10:00 Wasm: platformSynchronized is a no-op wrapper (single thread, no lock needed)
- 12:00 iOS: platformSynchronized maps to NSRecursiveLock for main-actor isolation
- 14:00 Testing Wasm safety: wasmJsTest source set and its limitations (no kotlinx-coroutines-test)
- 16:00 Common pitfalls: assuming blocking is safe on Wasm, deadlocking the event loop
- 17:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/PlatformSync.kt` -- expect declaration
- `shared/src/wasmJsMain/kotlin/digital/vasic/yole/` -- Wasm actual implementations
- `shared/src/wasmJsTest/kotlin/digital/vasic/yole/` -- Wasm test files

---

## Video 27.6: Protocol Service Non-Blocking Patterns (16 min)

### Timestamps
- 0:00 The 8 protocol services: each with its own coroutine scope and lifecycle
- 2:00 serviceScope pattern: SupervisorJob + Dispatchers.IO with cleanup on disconnect
- 4:00 scopeMutex: protecting scope creation and destruction from concurrent access
- 6:00 Connection limiter: Semaphore-based concurrency cap per protocol
- 8:00 Rate limiter: token bucket with suspend acquire() for back-pressure
- 10:00 Circuit breaker: state machine (Closed/Open/HalfOpen) with non-blocking transitions
- 12:00 Offline queue: suspending enqueue during network outage, draining on reconnect
- 14:00 Cancellation propagation: structured concurrency ensures disconnect cancels all in-flight ops
- 15:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/ConnectionLimiter.kt`
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/CircuitBreaker.kt`
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/RateLimiting.kt` -- Facade bridge

---

## Video 27.7: Testing Non-Blocking Guarantees (14 min)

### Timestamps
- 0:00 What a "non-blocking test" verifies: no thread is parked or blocked during the operation
- 2:00 Pattern: launch multiple concurrent operations, assert all complete within wall-clock bounds
- 4:00 Stress test example: 100 concurrent parse operations must finish within 2x single-parse time
- 6:00 Load test example: 1000 sequential cache lookups must maintain sub-millisecond p99
- 8:00 Cancellation test: cancelled parsing must release resources and not leave locked state
- 10:00 JUnit4 + runBlocking<Unit>: why Yole uses this pattern instead of runTest
- 12:00 Checking for regressions: performance baseline tests with thresholds
- 13:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/` -- Stress tests
- `shared/src/desktopTest/kotlin/digital/vasic/yole/` -- Desktop-specific non-blocking tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/` -- Load tests

---

## Video 27.8: Advanced Patterns and Future Directions (14 min)

### Timestamps
- 0:00 Flow-based reactive streams: StateFlow for config, SharedFlow for events
- 2:00 StateFlow.update{}: atomic emissions without external locking
- 4:00 Channel-based patterns: producer-consumer for batch operations
- 6:00 Asynchronous format detection: detecting format while the user is still typing
- 8:00 Incremental parsing: re-parsing only changed portions of a document
- 10:00 Web Worker integration: future offloading of heavy parsing to worker threads on Wasm
- 12:00 Kotlin/Native new memory model: implications for iOS non-blocking patterns
- 13:30 Summary

### Exercises
1. **Dispatcher audit**: Trace a file open operation from click to rendered HTML. Identify every dispatcher switch and explain why each is necessary.
2. **Cancellation test**: Write a test that starts parsing a large Markdown document, cancels it after 10ms, and verifies no leaked coroutines or locked Mutexes remain.
3. **Wasm safety**: Explain why a Wasm implementation of platformSynchronized can safely be a no-op, and describe a scenario where coroutine interleaving could still cause bugs.
4. **Performance measurement**: Add a benchmark that measures FormatRegistry lazy initialization time across 100 iterations and asserts the median is under 50ms.
