# B06 Anti-Bluff Audit — shared/commonTest network/ part 3 (protocols, stress)

**Auditor:** CONST-035/CONST-039 automated audit  
**Date:** 2026-05-20  
**Scope:** 32 files listed under `=== B06 ===` in batch-manifest.txt  
**Methodology:** Mutation-verification thought experiment — for each test method, ask: "If every line of the unit under test were replaced with a trivial stub, would this test still pass?" Any test that would still pass is BLUFF.

---

## Summary

| Verdict | Count |
|---------|-------|
| CLEAN   | 19    |
| SUSPECT | 5     |
| BLUFF   | 8     |

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/googledrive/GoogleDriveServiceEnhancedTest.kt

**Verdict: BLUFF**

Two tautological methods survive any trivial stub:

- `testEnhancedTestConnection()` line 111: `assertTrue(result.isSuccess || result.isFailure, "Test connection should complete")` — every `Result<T>` satisfies this; a stub returning `Result.success(Unit)` or `Result.failure(…)` equally passes. The surviving mutant: replace `testConnection()` with `return Result.failure(RuntimeException("stub"))` — assertion still passes.
- `testEnhancedQuotaInfo()` line 189: same tautology on `getQuotaInfo()` result. Surviving mutant: stub returns `Result.failure(…)` — assertion still passes.

Remaining methods (upload progress values, exact quota percentages, file counts) are genuinely behavioral. The two bluff methods are concentrated in the "when not connected" section.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/googledrive/GoogleDriveServiceTest.kt

**Verdict: BLUFF**

Two tautological methods:

- `testConnectSuccess()` line 53: `assertTrue(result.isSuccess || result.isFailure, ...)` — tautology. Surviving mutant: stub that throws immediately before returning would still cause `result` to be a failure, keeping the disjunction true.
- `testTestConnection()` line 306: same tautology. Surviving mutant identical.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/HttpProtocolsCoverageTest.kt

**Verdict: SUSPECT**

~70 test methods using Ktor `MockEngine`. The majority are strongly behavioral: URL content verified (`capturedUrl!!.contains("fullText")` line 226), exact document counts (`assertEquals(2, docs.size)` line 752), specific field values (`assertEquals(0.9, quota.usagePercentage)` lines 881-885). However three methods are near-bluff:

- `gdGetActiveOperations()` line 410: `assertNotNull(ops)` — a `List<NetworkOperation>` is never null in Kotlin; the assertion is vacuously true.
- `gdGetSyncStatus()` line 458: `assertNotNull(status)` — `Map<…>` is never null; always passes regardless of stub.
- `odGetQuotaInfoApiError()` line 669: `assertNotNull(result)` — `Result<T>` is never null.

These three methods would pass against a stub that returns any non-throwing value. The bulk of the file is CLEAN.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/NetworkProtocolStatusTest.kt

**Verdict: CLEAN**

All assertions verify concrete registry values: `assertEquals(8, protocols.size)` verifies a real count; specific protocol names and tiers are verified by name. A stub that omitted any protocol or changed a tier would fail multiple assertions.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/onedrive/OneDriveMockHttpTest.kt

**Verdict: CLEAN**

All tests use Ktor `MockEngine` to capture actual HTTP method, URL, and Authorization header values. Representative strong assertions: `assertEquals("Bearer test_token", capturedAuth)` line 149; `assertTrue(capturedUrl!!.contains("graph.microsoft.com"))` line 167; `assertEquals(2, docs.size)` line 233 with individual `doc.name` checks; `assertEquals(74.0, quota.usagePercentage, 0.01)` lines 621-622 with independent calculation. No tautologies found.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/onedrive/OneDriveServiceDeepTest.kt

**Verdict: BLUFF**

One tautological method:

- `testExistsOnDisconnectedServiceHandledGracefully()` line 381: `assertTrue(result.isSuccess || result.isFailure, ...)` — tautology. Surviving mutant: stub `exists()` to return `Result.failure(…)` — assertion still passes.

Remainder of the file (cache CRUD with state verification, exact `remotePath` equality) is CLEAN.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/onedrive/OneDriveServiceEnhancedTest.kt

**Verdict: BLUFF**

Two tautological methods:

- `testEnhancedTestConnection()` line 170: `assertTrue(result.isSuccess || result.isFailure, ...)` — tautology.
- `testEnhancedListFilesWhenConnected()` line 192: `assertTrue(result.isSuccess || result.isFailure, ...)` — tautology.

Surviving mutant for both: stub returns `Result.failure(…)` — assertions still pass.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/onedrive/OneDriveServiceTest.kt

**Verdict: BLUFF**

Two tautological methods:

- `testConnectSuccess()` line 55: `assertTrue(result.isSuccess || result.isFailure, ...)` — tautology.
- `testTestConnection()` line 328: `assertTrue(result.isSuccess || result.isFailure, ...)` — tautology.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/ProtocolDataClassTest.kt

**Verdict: CLEAN**

All assertions verify specific field values, equality semantics, `copy()` semantics, and `toString()` content on `FtpEntry`, `SmbEntry`, `SmbFileInfo`, `SmbShareInfo`, `SftpEntry`, `SftpFileAttributes`. A trivial stub that returned default/zero-initialized instances would fail most equality assertions.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/SafetyFixesTest.kt

**Verdict: BLUFF**

This file has four sections. Section 1 (cancellation) and Section 4 (lifecycle) are behavioral. Sections 2 and 3 are bluff:

**Section 2 — path traversal (~20 methods, lines 310–430):** Every method in this section uses the pattern `assertTrue(result.isSuccess || result.isFailure, "path traversal should be handled")`. The comment claims path traversal is being verified, but the assertion is a tautology regardless of whether traversal was blocked or allowed. Surviving mutant: replace `normalizePath()` with identity function — every traversal attempt succeeds, but all assertions still pass. Representative bluff lines: 316, 321, 325, 330, 334, 338, 342, 347, 356, 361, 365, 373, 376, 378, 380, 383, 386, 390, 426.

**Section 3 — search injection (~8 methods, lines 439–530):** All methods use `assertNotNull(flow)` on `Flow<…>` return values. Kotlin `Flow` instances returned from service calls are never null — a stub that returns `flowOf(Result.failure(…))` is non-null and satisfies every assertion. No assertion verifies that the injected query was sanitized.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/ServicePathUtilsTest.kt

**Verdict: CLEAN**

All 68 test methods verify specific string outputs from `getParentPath()` and `validatePath()` for all 8 protocol services (FTP, SFTP, SMB, Dropbox, GoogleDrive, OneDrive, Git, WebDAV). Representative: `assertEquals("/folder", result)`, `assertNull(result)`, `assertTrue(result.isFailure)` for blank paths. A stub `getParentPath` returning a fixed value would fail the majority of these.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/sftp/SftpServiceDeepCoverageTest.kt

**Verdict: CLEAN**

Uses an in-memory VFS simulation. Strongly behavioral: `listFiles returns default VFS files after connect` (lines 298-305) verifies specific file names; `uploadFile registers in VFS` (lines 341-347) verifies path and name after upload; `deleteFile removes from VFS` (lines 401-411) verifies state change; `connect initializes virtual file system` (lines 874-882) verifies `size=5242880L`. A stub VFS implementation that returned fixed data would fail name-specific and state-change assertions.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/sftp/SftpServiceEnhancedTest.kt

**Verdict: BLUFF**

One tautological method:

- `testEnhancedConnectionScenarios()` line 571: `assertTrue(timeoutResult.isSuccess || timeoutResult.isFailure, ...)` — tautology. Surviving mutant: stub `connect()` with any outcome.

Strong methods elsewhere (exact progress values 0.1, 0.4, 0.8; `bytesTransferred == totalSize` invariant) are CLEAN.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/sftp/SftpServiceTest.kt

**Verdict: CLEAN**

All methods assert specific failure types (`NetworkStorageException.ConnectionException.NotConnected`), exact error messages, or concrete state (`assertFalse(service.isOnline)` after connect failure). No tautologies found.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/smb/SmbServiceDeepCoverageTest.kt

**Verdict: CLEAN**

The strongest test file in the batch. Uses `testConnectFn`/`testAuthenticateFn` injection for real connect behavior. Key behavioral assertions: `delete folder removes all children` (lines 289-300) verifies VFS children absent; `rename folder updates all children paths` (lines 422-434) verifies path migration; `move migrates cache entries to new path` (lines 512-524) verifies cache key update; `getRecentChanges excludes files modified before since` (lines 851-857) verifies time-based filtering; `getQuotaInfo available plus used equals total` (lines 881-884) verifies arithmetic invariant.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/smb/SmbServiceEnhancedTest.kt

**Verdict: CLEAN**

All tests use `testConnectFn`/`testAuthenticateFn` injection. Behavioral assertions: `assertTrue(result.isSuccess)` paired with `assertTrue(service.isOnline)` state checks (lines 99-100); `assertEquals("/shared/dest/file.txt", document.path)` (line 316); `assertEquals(SyncStatus.SYNCED, document.syncStatus)` (line 318); `assertEquals(quota.totalSpace, quota.usedSpace + quota.availableSpace)` (line 545). No tautologies found; all `isSuccess` assertions are on `Result<T>` that could legitimately fail if the service were not properly initialized.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/smb/SmbServiceTest.kt

**Verdict: CLEAN**

Tests use the real `SmbService` constructor (no inject). `testConnectFailsWithInvalidHost()` line 55 asserts `result.isFailure` — genuine behavioral assertion that would fail if the service incorrectly returned success. `testSyncFileWhenNotConnected()` lines 255-267 asserts exact count (4 operations), exact progress values (0.0, 0.5, 1.0), and exact statuses. `testGetQuotaInfoWhenNotConnected()` lines 172-175 asserts exact long values (1000000000L, 100000000L, 900000000L, 0.1). No tautologies.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/webdav/WebDavMockHttpTest.kt

**Verdict: CLEAN**

~130 test methods with Ktor `MockEngine`. Comprehensive behavioral coverage: PROPFIND method verified (`assertEquals(HttpMethod("PROPFIND"), capturedMethod)` line 386); Depth header verified (`assertEquals("1", capturedDepth)` line 402); exact Basic auth credentials verified via Base64 decode (lines 453-454); exact document count 3 from multistatus XML (line 309); `assertEquals(1073741824L, quota.usedSpace)` (line 1748) from parsed XML; `assertEquals(0, parentEntries.size)` (line 502) verifying parent entry exclusion. No tautologies.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/webdav/WebDavServiceDeepTest.kt

**Verdict: CLEAN**

Tests the non-HTTP state-management layer. Strongly behavioral: `addToCache then getCacheEntries returns entry` (line 283) verifies `entries[0].remotePath == "/file.txt"`; `getCacheEntries filters by path prefix` (lines 300-304) verifies count=1; `syncFile updates sync status to SYNCED` (lines 380-384) verifies `status.containsKey("/docs/file.txt")` and `status["/docs/file.txt"] == SyncStatus.SYNCED`; quota arithmetic invariant verified (line 503). No tautologies.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/webdav/WebDavServiceEnhancedTest.kt

**Verdict: SUSPECT**

Two methods assert a disjunction over status (not a boolean tautology, but a coverage gap):

- `uploadFile succeeds when connected()` lines 196-198: `assertTrue(lastResult.status == NetworkOperation.Status.FAILED || lastResult.status == NetworkOperation.Status.COMPLETED)` — this two-value disjunction covers all possible terminal statuses, so a stub that emits either terminal status passes. Surviving mutant: stub `uploadFile` to emit `FAILED` — assertion still passes.
- `downloadFile succeeds when connected()` lines 235-237: identical pattern.

These are SUSPECT (not BLUFF) because the test was written to acknowledge lack of a real server and both branches are documented. The remainder of the file is strongly behavioral (exact exception types for disconnected operations, exact path/name values, exact syncAll error message on line 441).

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/webdav/WebDavServiceTest.kt

**Verdict: CLEAN**

All "when not connected" tests verify specific error messages (`assertEquals("WebDAV not connected", ...)` lines 83, 95, 105, 232); `testSyncFileWhenNotConnected()` lines 254-258 verifies exact operation type (SYNC) and exact status (COMPLETED); `testSyncAllWhenNotConnected()` lines 271-274 verifies exact FAILED status and exact error message. No tautologies.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/RateLimitedStorageServiceTest.kt

**Verdict: CLEAN**

All tests use `RateLimitMockService` with call tracking. Every test verifies both the delegated result AND that the specific method appears in `mock.calls`. Representative: `listFilesDelegatesAndForwardsResults()` verifies `results[0].getOrNull()?.size == 2`, `results[0].getOrNull()?.get(0)?.name == "file1.txt"`, `mock.calls.contains("listFiles")`, and `mock.lastListFilesPath == "/documents"`. Delegation failures are explicitly tested (e.g., `connectForwardsFailure()` lines 451-455 verifies exact error message). The call-tracking mechanism means a stub that didn't actually delegate would fail the `mock.calls` assertions.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/StorageQuotaTest.kt

**Verdict: CLEAN**

All tests verify specific field values on `StorageQuota` data class: exact long values (1073741824L), formatted strings ("512B", "2KB", "5MB", "10GB", "1MB", "3KB"), percentage strings ("75%", "0%", "100%"), `expiresAt` identity, `metadata` map lookups, `copy()` semantics. A stub that returned default/zero-initialized `StorageQuota` would fail the formatted-string assertions.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/ConcurrencyStressTest.kt

**Verdict: SUSPECT**

Stress tests primarily verify absence of deadlock, not behavioral correctness:

- `testConcurrentConnectDisconnect()` line 51 verifies `storageInfo.id == "smb_stress-test"` — genuine, would fail if ID generation were broken.
- `testConcurrentStateAccess()` lines 61-74: `assertEquals(50, results.size)` — verifies all coroutines completed, but `results` is the list of `Boolean` return values from `service.isOnline`, which is always `true` or `false`. The assertion verifies count only, not absence of data races.
- `testConnectDisconnectLeavesConsistentState()` line 126: `assertTrue(service.isOnline)` after each connect is genuinely behavioral; `assertTrue(!service.isOnline)` after final disconnect is meaningful.

Overall SUSPECT because most tests verify "completed without throwing" rather than correctness of state, but the framework's `withTimeout` and `awaitAll` provide real deadlock detection value.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/DatabaseStressTest.kt

**Verdict: SUSPECT**

All test methods follow the pattern `assertEquals(N, results.size)` and `assertTrue(results.all { it.isSuccess })`. These assertions verify that N concurrent operations completed without throwing, but they do not verify the database state after the operations (e.g., after 100 concurrent insertions, the test does not query the DB to verify 100 records exist). A stub `insertStorage` that returned `Result.success(Unit)` without inserting anything would pass all stress tests. The tests are valuable as deadlock/crash detectors but not as behavioral correctness proofs. SUSPECT rather than BLUFF because `InMemoryTestDatabase` is a real in-memory implementation (not a mock), so the operations do exercise real code paths — just without outcome verification.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/NetworkProtocolStressTest.kt

**Verdict: CLEAN**

Several methods provide genuine behavioral assertions beyond "completed":

- `FTP rapid file info requests()` line 99: `assertTrue(connectResult.isFailure)` — specific directional assertion that real FTP connection to non-existent server fails.
- `FTP rapid file info requests()` line 99-100: `assertTrue(results.all { it.isFailure })` — verifies all 100 fail, not just some.
- `Git path validation stress()` line 345: `assertTrue(results.all { it.isSuccess })` — 200 well-formed paths all succeed.

The concurrent count-equality assertions (`assertEquals(10, results.size)`) are trivially true (awaitAll never drops results), but the file also contains genuine failure-direction assertions. CLEAN overall.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/ParserOverloadStressTests.kt

**Verdict: CLEAN**

Strong behavioral assertions alongside no-crash verification:

- `TaskpaperLargeDocument500TasksNoCrash()` line 231: `assertEquals("500", doc.metadata["tasks"])` — verifies real parse result, not just non-null.
- `ConcurrentMarkdownResultsAreConsistent()` line 99: `assertEquals(firstId, it.format.id)` for all 50 results — verifies determinism of concurrent parsing.
- `AllEighteenFormatsParsedConcurrentlyNoCrash()` lines 133-136: `assertEquals(18, results.size)` combined with all-`assertNotNull` — verifies each parser returned a result.
- `MarkdownLargeDocument10KLinesNoCrash()` line 196: `assertTrue(doc.rawContent.length > 100_000)` — verifies actual parse volume.

No tautologies.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/PerProtocolStressTests.kt

**Verdict: CLEAN**

The key test design point is: each method under deadlock/failure verification uses `withTimeout(30.seconds)` wrapping, making the `awaitAll()` a real deadlock detector. Beyond count assertions:

- `FtpConcurrentGetFileInfoAllFailGracefully()` line 140: `assertTrue(results.all { it.isFailure })` — specific directional failure assertion, not just count.
- `SftpConcurrentGetFileInfoAllFailGracefully()` line 170: same pattern.
- `SmbConcurrentGetFileInfoAllFailGracefully()` line 201: same pattern.
- `AllProtocolsConcurrentGetFileInfoNoDeadlock()` line 419: `assertEquals(80, results.size)` plus each-result `assertNotNull`.

The `assertNotNull(service)` calls on service instances (line 117, 162, etc.) are vacuous (service is always non-null after construction), but they are incidental. The `withTimeout` wrapping provides the real behavioral value.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/ProtocolOverloadStressTest.kt

**Verdict: CLEAN**

This is the most behaviorally rigorous stress test file. Key assertions:

- `CircuitBreaker trips after failure threshold` (line 47): `assertEquals(CircuitBreaker.State.OPEN, cb.state)` — specific state assertion.
- `CircuitBreaker stays CLOSED when failures below threshold` (line 69-72): result value asserted (`assertEquals("success", result.getOrNull())`).
- `CircuitBreaker success resets failure counter` (line 158): `assertEquals(0, cb.failures)` — internal counter verified.
- `CircuitBreaker tracks total call count under load` (lines 179-181): `assertEquals(totalCalls.toLong(), cb.calls)` and `assertEquals(totalCalls, cb.successes)`.
- `ConnectionLimiter limits max concurrent operations` (line 208): `assertTrue(maxObservedConcurrent <= 3)` using real Mutex instrumentation.
- `ConnectionLimiter operations complete fully under sustained load` (line 271): `assertEquals(100, completedCount)`.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/RateLimiterSaturationTest.kt

**Verdict: CLEAN**

All assertions are behavioral invariants, not trivial:

- `RateLimiter handles sustained saturation without starvation()` line 55: `assertEquals(totalRequests, completedCount)` — verifies no starvation.
- `RateLimiter throughput is bounded by concurrency limit()` line 79: `assertTrue(maxObservedConcurrency <= maxConcurrent)` using real atomic instrumentation.
- `RateLimiter queue drains completely after saturation()` lines 101-102: `assertEquals(0, limiter.getActiveCount())` and `assertEquals(0, limiter.getQueueLength())`.
- `RateLimiter executeWithTimeout returns null under full saturation()` line 129: `assertTrue(timedOutCount > 0)`.
- `TokenBucket rapid drain and verify depletion()` lines 145-146: `assertEquals(capacity, acquired)` and `assertFalse(bucket.tryAcquire())`.
- `OperationThrottler enforces limit under rapid fire()` lines 274-275: exact `assertEquals(20, allowed)` and `assertEquals(80, blocked)`.
- `OperationThrottler independent operation IDs do not interfere()` lines 310-311: exact counts for two independent keys.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/ResourceManagementStressTest.kt

**Verdict: CLEAN**

Key behavioral assertions:

- `repeated connect-disconnect cycles do not leak resources()` lines 47-50: `assertTrue(connectResult.isFailure)` for real FTP — directional assertion; `assertFalse(service.isOnline)` after failed connect.
- `many services created and destroyed concurrently()` line 145: `assertTrue(services.all { !it.isOnline })` — verifies all 100 services are offline after `disconnect()`.
- `many simultaneous file info requests()` lines 166-167: `assertTrue(results.all { it.isFailure })` — all 200 fail when not connected.
- `rapid storage info requests()` line 253: `assertTrue(infos.all { it.type == StorageType.WEBDAV })` — verifies type of each storage info.
- `concurrent connect-disconnect on same service()` line 121: `assertNotNull(finalState.toString())` — this particular assertion is vacuous (Boolean.toString() is never null), SUSPECT sub-point, but the surrounding test has legitimate deadlock-detection value.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/TimeoutRecoveryTests.kt

**Verdict: CLEAN**

The most rigorous timing/resilience test file. Every method asserts specific state:

- `CircuitBreakerBlocksCallsWhenOpen()` lines 58-60: asserts `result.exceptionOrNull() is CircuitBreakerOpenException` — specific type check.
- `CircuitBreakerSuccessResetsFailureCount()` line 72: `assertEquals(0, cb.failures)`.
- `CircuitBreakerCountersAreTracked()` lines 123-125: exact counters `assertEquals(3, cb.successes)`, `assertEquals(2, cb.failures)`, `assertEquals(5L, cb.calls)`.
- `CircuitBreakerCancellationExceptionNotCounted()` lines 186-189: `assertEquals(CircuitBreaker.State.CLOSED, cb.state)` and `assertEquals(0, cb.failures)` — verifies CancellationException special-casing.
- `ConnectionLimiterPermitsReleasedAfterCompletion()` line 222: `assertEquals(3, limiter.availablePermits)` after 10 sequential operations.
- `ConnectionLimiterPermitsReleasedOnException()` line 233: same permits check after exception path.
- `CircuitBreakerTripDoesNotBlockConnectionLimiter()` lines 307-309: verifies independent operation of CB and limiter.

---

## Cross-file Bluff Pattern Summary

The dominant bluff pattern across all 8 BLUFF files is:

```kotlin
assertTrue(result.isSuccess || result.isFailure, "some description")
```

This is a Kotlin type tautology — `Result<T>` is a sealed class with exactly two subtypes (`Success` and `Failure`), so the disjunction is always `true`. It appears in:

1. `GoogleDriveServiceEnhancedTest` — 2 methods
2. `GoogleDriveServiceTest` — 2 methods  
3. `OneDriveServiceDeepTest` — 1 method
4. `OneDriveServiceEnhancedTest` — 2 methods
5. `OneDriveServiceTest` — 2 methods
6. `SftpServiceEnhancedTest` — 1 method
7. `SafetyFixesTest` — ~20 path-traversal methods, ~8 search-injection methods

The `SafetyFixesTest` path-traversal section is the most dangerous: it claims to verify that path traversal is blocked, but the tautological assertions provide zero evidence that normalization actually rejected anything. An `isSuccess` result could mean the traversal was allowed.

Secondary bluff pattern: `assertNotNull(flow)` on `Flow<…>` return values (SafetyFixesTest search-injection section) — Kotlin flows are never null.

---

## Recommendations

1. **Path-traversal section of SafetyFixesTest** (highest priority): Replace all `assertTrue(result.isSuccess || result.isFailure)` with `assertTrue(result.isFailure)` when testing that traversal is blocked, or `assertEquals(normalized, result.getOrThrow())` when testing that the path was sanitized to the expected value.

2. **Google/OneDrive `testConnectSuccess()` / `testTestConnection()` patterns**: Replace with `assertTrue(result.isSuccess)` after verifying the test infrastructure actually makes the connect succeed, or `assertTrue(result.isFailure)` for the unreachable-host variant.

3. **`SafetyFixesTest` search-injection section**: Replace `assertNotNull(flow)` with an assertion that the flow emits a specific failure or that the sanitized query appears in the results.

4. **`HttpProtocolsCoverageTest` near-bluffs**: Replace `assertNotNull(ops)`, `assertNotNull(status)`, `assertNotNull(result)` with directional assertions (e.g., `assertTrue(ops.isEmpty())` or `assertEquals(expectedCount, ops.size)`).
