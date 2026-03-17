<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Module 22: Concurrency Safety in KMP (8 videos)

## Video 22.1: Concurrency Challenges in Kotlin Multiplatform (18 min)

### Timestamps
- 0:00 Introduction: why concurrency in KMP is different from JVM-only development
- 2:00 The platform constraint: `java.util.concurrent.*` is unavailable in commonMain
- 4:00 KMP target differences: JVM threads, iOS GCD, Wasm single-threaded, Desktop JVM
- 6:00 The kotlinx.coroutines solution: cross-platform concurrency primitives
- 8:00 Yole's concurrency requirements: 8 protocol services, 17 parsers, shared state
- 10:00 Common mistakes: using Thread, AtomicInteger, System.currentTimeMillis in commonMain
- 12:00 The approved primitives: Mutex, Semaphore, @Volatile, StateFlow, synchronized, delay
- 14:00 Clock.System.now() as the KMP-compatible timing function
- 16:00 Testing concurrency: runBlocking<Unit> vs. runTest (JUnit4 constraint)
- 17:30 Summary and what we will build in this module

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/RateLimiting.kt`
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/LazyLoading.kt`
- `docs/CONCURRENCY_SAFETY.md`

---

## Video 22.2: Mutex and Lock Ordering (20 min)

### Timestamps
- 0:00 Mutex basics: withLock, suspend-friendly, no thread blocking
- 2:00 Why Yole needs 8 different mutexes per protocol service
- 4:00 The lock ordering convention: scopeMutex(1) > stateMutex(2) > ... > storageInitMutex(8)
- 6:00 Correct multi-lock acquisition: always ascending order
- 8:00 The disconnect() pattern: scopeMutex first, then stateMutex, then operationsMutex
- 10:00 Why I/O must never happen inside a lock: preventing coroutine starvation
- 12:00 Leaf locks: activeJobsMutex and storageInitMutex never hold other locks
- 14:00 Common deadlock patterns and how lock ordering prevents them
- 16:00 Verifying lock ordering: ConcurrencySafetyTest and ComprehensiveStressTests
- 18:00 Live coding: adding a new mutex to a protocol service following the ordering
- 19:30 Summary

### Code References
- `docs/LOCK_ORDERING.md`
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/dropbox/DropboxService.kt`
- `shared/src/commonTest/kotlin/digital/vasic/yole/util/ConcurrencySafetyTest.kt`

---

## Video 22.3: @Volatile and Lazy Initialization Patterns (18 min)

### Timestamps
- 0:00 @Volatile in KMP: visibility guarantee across platforms
- 2:00 ParsedDocument's lazy HTML caching with @Volatile
- 4:00 The idempotency requirement: why duplicate computation is acceptable
- 6:00 When @Volatile is sufficient vs. when you need Mutex
- 8:00 Kotlin's `by lazy { }` for thread-safe one-time initialization
- 10:00 HttpClient lazy initialization with the _httpClientAccessed guard pattern
- 12:00 FormatRegistry.formats: lazy { createFormats() } and isFormatsInitialized
- 14:00 Parser lazy registration: factory lambdas vs. eager instantiation
- 16:00 StyleSheets.styleSheetCache: caching with clearCache()
- 17:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextParser.kt` -- ParsedDocument
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt`
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/StyleSheets.kt`
- `docs/LAZY_LOADING.md`

---

## Video 22.4: StateFlow for Reactive State Management (15 min)

### Timestamps
- 0:00 StateFlow vs. SharedFlow vs. Channel: when to use each
- 2:00 MutableStateFlow in NetworkStorageConfigService for configuredStorages
- 4:00 The atomic update pattern: mutex.withLock { _state.value = transform(value) }
- 6:00 Pause flags with MutableStateFlow<Boolean> per operation
- 8:00 Collecting StateFlow in UI: Compose collectAsState()
- 10:00 Testing StateFlow: verifying emissions in runBlocking<Unit>
- 12:00 Common pitfall: updating StateFlow without Mutex (lost updates)
- 14:00 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/config/NetworkStorageConfigService.kt`

---

## Video 22.5: Semaphore and Rate Limiting (18 min)

### Timestamps
- 0:00 Semaphore for concurrency control: limiting parallel operations
- 2:00 ConnectionLimiter: capping concurrent connections per protocol service
- 4:00 RateLimiter: Semaphore + Mutex for operation tracking
- 6:00 TokenBucket: time-based rate limiting with KMP-compatible Clock.System.now()
- 8:00 AdaptiveRateLimiter: self-tuning based on success/failure patterns
- 10:00 OperationThrottler: per-operation-ID throttling with time windows
- 12:00 RateLimitedStorageService: decorator pattern wrapping any NetworkStorageService
- 14:00 Testing rate limiters under load: StressAndIntegrationTest
- 16:00 Tuning semaphore permits for different server characteristics
- 17:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/RateLimiting.kt`
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/ConnectionLimiter.kt`

---

## Video 22.6: SupervisorJob and Structured Concurrency (15 min)

### Timestamps
- 0:00 Why structured concurrency matters: preventing coroutine leaks
- 2:00 SupervisorJob in FlowLazyLoader: isolating failures
- 4:00 serviceScope lifecycle: creation, use, cancellation, recreation
- 6:00 The scopeMutex pattern: protecting scope lifecycle operations
- 8:00 CancellationException: the golden rule -- always rethrow
- 10:00 Testing scope cleanup: MemoryLeakDetectionTest patterns
- 12:00 Background file sync with structured concurrency
- 14:00 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/LazyLoading.kt` -- FlowLazyLoader
- `shared/src/commonTest/kotlin/digital/vasic/yole/util/MemoryLeakDetectionTest.kt`

---

## Video 22.7: CancellationException Handling (15 min)

### Timestamps
- 0:00 CancellationException is special: must never be swallowed
- 2:00 The pattern: catch(e: Exception) { if (e is CancellationException) throw e }
- 4:00 All 8 protocol services enforce this rule in every catch block
- 6:00 Flow CancellationException: special handling in getRecentChanges()
- 8:00 What happens when you swallow CancellationException: coroutine becomes uncancellable
- 10:00 Auditing catch blocks: searching the codebase for violations
- 12:00 Writing tests that verify cancellation propagation
- 14:00 Summary

### Code References
- All 8 protocol service files in `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/`

---

## Video 22.8: Concurrency Testing Patterns (20 min)

### Timestamps
- 0:00 The testing challenge: concurrency bugs are non-deterministic
- 2:00 ConcurrencySafetyTest: 100 coroutines x 100 increments pattern
- 4:00 Testing for races: check-then-act patterns with Mutex
- 6:00 Memory leak detection: FlowLazyLoader scope cancellation
- 8:00 Stress testing: 100+ concurrent requests to rate limiters
- 10:00 Non-blocking verification: withTimeout for blocking detection
- 12:00 The JUnit4 constraint: runBlocking<Unit> for all concurrency tests
- 14:00 Avoiding flaky tests: deterministic seeds, bounded inputs, yield()
- 16:00 ConcurrencyFixesTest: 1006-line test file for the 10 critical fixes
- 18:00 Integrating concurrency tests into CI
- 19:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/util/ConcurrencySafetyTest.kt`
- `shared/src/commonTest/kotlin/digital/vasic/yole/util/StressAndIntegrationTest.kt`
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/nonblocking/NonBlockingOperationTests.kt`

### Exercises
1. **Write a race condition test** -- Create two coroutines that both read-modify-write a shared counter without Mutex, observe the race, then add Mutex and verify correctness.
2. **Audit CancellationException handling** -- Search all catch blocks in the codebase for potential CancellationException swallowing.
3. **Lock ordering violation detection** -- Write a test that intentionally violates lock ordering and explain why it could deadlock.
