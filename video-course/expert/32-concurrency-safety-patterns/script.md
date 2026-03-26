<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Module 32: Concurrency Safety Patterns in KMP (8 videos)

## Prerequisites

- Module 9: Advanced Architecture Patterns
- Module 22: Concurrency Safety in KMP
- Module 11: Testing Strategies

## Learning Objectives

- Apply @Volatile correctly to shared fields in KMP protocol services
- Enforce lock ordering across 8 mutex priorities to prevent deadlocks
- Configure HttpTimeout in platform HttpClientFactory implementations
- Write concurrency regression tests that catch races before they reach production
- Audit an existing codebase for missing @Volatile and lock ordering violations

---

## Video 32.1: Why @Volatile Still Matters in KMP (14 min)

### Timestamps
- 0:00 Introduction: the visibility problem in multi-threaded Kotlin
- 2:00 What @Volatile guarantees: visibility across threads, not atomicity
- 4:00 The JVM memory model: without @Volatile, writes may stay in CPU cache
- 6:00 KMP targets: @Volatile on JVM, iOS (Kotlin/Native), and Desktop all behave differently
- 8:00 Case study: _isConnected field in protocol services -- why it needs @Volatile
- 10:00 Case study: _rootPath and _rootFolderId -- lazy-set fields that are read from multiple coroutines
- 12:00 When @Volatile is NOT enough: compound operations still need a Mutex
- 13:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/` -- All 8 protocol service files
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt` -- parseSemaphore field

---

## Video 32.2: Auditing a Codebase for Missing @Volatile (14 min)

### Timestamps
- 0:00 The audit approach: what to look for and where
- 2:00 Pattern 1: fields assigned in one coroutine and read in another without a lock
- 4:00 Pattern 2: lazy-initialized fields using custom null-check patterns instead of `by lazy {}`
- 6:00 Pattern 3: boolean flags (isConnected, isInitialized) modified outside of withLock blocks
- 8:00 Running the audit on all 8 protocol services: the grep strategy
- 10:00 Fixing the findings: adding @Volatile to _isConnected, _rootPath, _rootFolderId, parseSemaphore
- 12:00 Writing a regression test that would have caught the missing @Volatile
- 13:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/DropboxService.kt`
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/GoogleDriveService.kt`
- `shared/src/commonTest/kotlin/digital/vasic/yole/` -- Concurrency regression tests

---

## Video 32.3: Lock Ordering to Prevent Deadlocks (16 min)

### Timestamps
- 0:00 Introduction: what is a deadlock and how lock ordering prevents it
- 2:00 The 8-mutex priority order in Yole: scopeMutex > stateMutex > operationsMutex > syncMutex > cacheMutex > pauseFlagsMutex > activeJobsMutex > storageInitMutex
- 4:00 How a lock ordering violation occurs: acquiring mutexes in different orders in different code paths
- 6:00 Case study: cancelOperation in Dropbox/GoogleDrive/OneDrive/FTP acquiring locks out of order
- 8:00 The fix: always acquire in the canonical order; release in reverse order
- 10:00 Detecting violations: code review patterns and static analysis
- 12:00 Documenting the ordering: docs/LOCK_ORDERING.md as the authoritative reference
- 14:00 Testing lock ordering: stress tests that spawn concurrent operations to surface deadlocks
- 15:30 Summary

### Code References
- `docs/LOCK_ORDERING.md` -- The authoritative lock ordering reference
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/FtpService.kt` -- cancelOperation fix
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/DropboxService.kt` -- cancelOperation fix

---

## Video 32.4: HttpTimeout Configuration Across Platforms (12 min)

### Timestamps
- 0:00 Why timeouts matter: hung connections block coroutines and exhaust thread pools
- 2:00 Ktor's HttpTimeout plugin: connectTimeoutMillis, requestTimeoutMillis, socketTimeoutMillis
- 4:00 The platform HttpClientFactory pattern: expect/actual for platform-specific configuration
- 6:00 Configuring HttpTimeout in Android HttpClientFactory (10s connect, 30s request)
- 8:00 Configuring HttpTimeout in Desktop HttpClientFactory
- 9:00 Configuring HttpTimeout in iOS HttpClientFactory
- 10:00 Testing timeout behavior: MockEngine with delayed responses
- 11:30 Summary

### Code References
- `shared/src/androidMain/kotlin/digital/vasic/yole/network/platform/HttpClientFactory.android.kt`
- `shared/src/desktopMain/kotlin/digital/vasic/yole/network/platform/HttpClientFactory.desktop.kt`
- `shared/src/iosMain/kotlin/digital/vasic/yole/network/platform/HttpClientFactory.ios.kt`

---

## Video 32.5: CancellationException Rethrow Rules (12 min)

### Timestamps
- 0:00 The rule: CancellationException must always be rethrown in catch blocks
- 2:00 Why: structured concurrency relies on CancellationException propagating up the scope chain
- 4:00 What happens when you swallow it: coroutines that never cancel, scope leaks, test hangs
- 6:00 Auditing catch blocks across 8 protocol services
- 8:00 The correct pattern: catch (e: Exception) { if (e is CancellationException) throw e; ... }
- 10:00 Detekt rule SwallowedException: enforcing the pattern at the static analysis level
- 11:30 Summary

### Code References
- `config/detekt/detekt.yml` -- SwallowedException rule configuration
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/` -- Protocol service catch blocks

---

## Video 32.6: SupervisorJob and Structured Concurrency (14 min)

### Timestamps
- 0:00 Structured concurrency in KMP: why every coroutine needs a parent scope
- 2:00 CoroutineScope vs GlobalScope: why GlobalScope is banned in Yole (Detekt enforces this)
- 4:00 SupervisorJob: child failures don't cancel siblings
- 6:00 The serviceScope pattern in protocol services: creation, lifecycle, and teardown
- 8:00 scopeMutex protecting scope creation and destruction
- 10:00 Cancelling the serviceScope on disconnect: ensuring all in-flight operations complete
- 12:00 Testing scope lifecycle: verifying operations cancel cleanly on disconnect
- 13:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/` -- serviceScope pattern
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/LazyLoading.kt` -- FlowLazyLoader SupervisorJob

---

## Video 32.7: Concurrency Regression Test Patterns (16 min)

### Timestamps
- 0:00 The goal: tests that reliably catch races before they reach production
- 2:00 Pattern 1: concurrent connect/disconnect stress test
- 4:00 Pattern 2: concurrent read/write to @Volatile fields via coroutines
- 6:00 Pattern 3: lock ordering test -- spawn operations that acquire mutexes in all combinations
- 8:00 Pattern 4: cancellation test -- cancel a scope mid-operation and verify cleanup
- 10:00 Using runBlocking<Unit> correctly in JUnit4 (not runTest which returns TestResult)
- 12:00 Parameterized stress tests: varying thread counts and iteration counts
- 14:00 Interpreting failures: race condition signatures vs logic bugs
- 15:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/` -- ConcurrencyFixesTest.kt and Session 7 regression tests
- `shared/src/desktopTest/kotlin/digital/vasic/yole/` -- MockK-based concurrency tests

---

## Video 32.8: Applying Concurrency Patterns to New Services (14 min)

### Timestamps
- 0:00 Checklist for a new protocol service: all required concurrency patterns
- 2:00 Step 1: define all Mutex fields in canonical lock order
- 4:00 Step 2: mark all shared mutable fields with @Volatile or protect with Mutex
- 6:00 Step 3: create serviceScope with SupervisorJob, protect with scopeMutex
- 8:00 Step 4: rethrow CancellationException in all catch blocks
- 10:00 Step 5: add HttpTimeout to HttpClientFactory for the target platform
- 12:00 Step 6: write a concurrency regression test before submitting for review
- 13:30 Summary

### Exercises
1. **@Volatile audit**: Run a grep for `private var _` across all protocol services. Identify any field not marked @Volatile that is assigned in one coroutine and read in another.
2. **Lock ordering verification**: List all withLock calls in DropboxService in order of appearance. Verify they match the canonical ordering in docs/LOCK_ORDERING.md.
3. **Timeout test**: Write a test using MockEngine that delays a response by 35 seconds and verify the request times out with an appropriate exception.
4. **Cancellation test**: Write a test that starts a long-running operation in a serviceScope, cancels the scope mid-operation, and asserts all resources are released cleanly.
5. **New service checklist**: Apply the 6-step checklist to a hypothetical new S3Service. Document which mutex fields you would define and in what order.
