# B05 Audit — Network (Part 2)
<!-- CONST-035 / CONST-039 Anti-Bluff Audit — Phase 5 -->

**Batch:** B05 (32 files)
**Auditor role:** CONST-035 / CONST-039 mutation-verification thought experiment
**Audit date:** 2026-05-20
**Rule applied:** "If every line of the unit under test were replaced with a trivial stub
(return null / return 0 / return emptyList / return '' / no-op), would this test still pass?"

---

## Legend

| Verdict | Meaning |
|---------|---------|
| CLEAN | All or nearly all assertions would catch a trivial stub substitution. Real behaviour is exercised. |
| SUSPECT | Mix of real assertions and tautological / too-weak assertions. Some tests would survive a trivial stub. |
| BLUFF | Majority of tests would pass even if the unit under test were a no-op stub. No positive evidence of real behaviour. |

---

### 1. NetworkIntegrationComprehensiveTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/NetworkIntegrationComprehensiveTest.kt`

**Verdict: BLUFF**

**Reasoning:**
- `simulateNetworkOperationWithProgress` is a locally-defined flow that always emits `COMPLETED` — it is not connected to any production code path.
- `simulateNetworkError` copies an operation data class with `FAILED` status — no production code is called.
- `testConcurrentOperationsAcrossProtocols` and `testPerformanceUnderLoad` manipulate local `NetworkOperation` data classes only; none of the real protocol services are invoked.
- `testAuthenticationFlowIntegration` is the lone exception: it uses a real `AuthTokenManager` backed by `IntegrationTestSecureStorage` (an in-memory map) and verifies that `retrieveToken()` returns the stored value. This test would catch a stub.
- Every other test contains either `assertTrue(op.isSuccess || op.isFailure)` (tautological) or assertions on fields of locally-constructed data objects that never exercise production code.

**Affected tests (representative):** `testParallelFileOperationsIntegration`, `testNetworkResilience`, `testProgressTracking`, `testSyncStatusLifecycle`, `testOperationCancellation`, `testPerformanceUnderLoad`, `testConcurrentOperationsAcrossProtocols`.

**Fix:** Replace local `simulate*` helpers with real service calls (even against disconnected services), and assert on the specific exception type and message returned by the real protocol services, not on data-class fields set by the test itself.

---

### 2. NetworkPerformanceTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/NetworkPerformanceTest.kt`

**Verdict: BLUFF**

**Reasoning:**
- `simulateNetworkOperation` does `delay(Random.nextLong(1, 10))` and returns `Result.success(true)` unconditionally — it is not connected to any production code.
- `simulateUpload`, `simulateDownload`, `simulateFileInfo`, `simulateListDirectory`, `simulateCreateFolder`, `simulateDeleteFile` are all `delay()` wrappers returning success.
- All performance benchmarks (`testNetworkThroughput`, `testConcurrentPerformance`, `testLatencyDistribution`, etc.) measure timing of these stubs; the production code under the nominal "NetworkStorageService" interface is never invoked.
- `MockPerformanceDatabase` is an in-memory HashMap that mimics nothing in production.
- Replace every stub with stubbed `Result.failure(...)` and every test still passes.

**Fix:** Benchmark real protocol services (e.g. `FtpService.connect()`, `DropboxService.exists()`). Assert on latency measured from those real calls, not from local `delay()` wrappers.

---

### 3. NetworkServiceCoverageTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/NetworkServiceCoverageTest.kt`

**Verdict: CLEAN** (with one minor SUSPECT method)

**Reasoning:**
- `StorageQuota.formatBytes` tests: `assertEquals("0B", quota.formattedTotalSpace)`, `assertEquals("1KB", ...)`, `assertEquals("1MB", ...)`, `assertEquals("100GB", ...)` — all specific string assertions that would catch a stub returning wrong output.
- `NetworkStorageException.fromThrowable` dispatch tests: keyword matching for "timeout" → `Timeout`, "permission" → `PermissionDenied`, etc. Real dispatch logic is exercised.
- `toUserMessage()`, `isRetryable()`, `isPermanentFailure()` all have concrete boolean/string assertions.
- `NetworkStorageConfigService.getSupportedStorageTypes()` verifies specific named services appear in each type category.
- Minor SUSPECT: `getSuggestedAction` assertions check `assertNull(...)` on some variants without verifying why — these would survive if the method simply always returned `null`.

**Fix (minor):** For `getSuggestedAction`, add assertions that verify it returns a non-null, non-empty string for at least the retryable exceptions.

---

### 4. NetworkStorageErrorUnitTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/NetworkStorageErrorUnitTest.kt`

**Verdict: CLEAN**

**Reasoning:**
- 50 tests cover the full `NetworkStorageException` hierarchy.
- `assertEquals("CONNECTION_TIMEOUT", ex.errorCode)` — a stub returning a different code would fail.
- Error message content checks: `assertTrue(msg.contains("timed out", ignoreCase=true) || msg.contains("internet", ignoreCase=true))` — real message generation is exercised.
- `fromThrowable` keyword dispatch: "Connection timeout exceeded" → `Timeout`, "Authentication failed" → `Authentication`, "quota exceeded" → `QuotaExceeded`, etc.
- `isRetryable()`: `Timeout` → `true`, `Authentication` → `false` — these would catch a stub returning a constant.
- `isPermanentFailure()`: `Authentication` → `true`, `QuotaExceeded` → `true`, `Timeout` → `false`.
- Cause-chain tests: `assertEquals(originalCause, ex.cause)`.

No significant bluff patterns found.

---

### 5. NetworkStorageIntegrationTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/NetworkStorageIntegrationTest.kt`

**Verdict: SUSPECT**

**Reasoning:**
- Real service instances are created (`DropboxService`, `GoogleDriveService`, `FtpService`, etc.).
- Many assertions are `assertTrue(result.isSuccess || result.isFailure)` at lines 159, 193, 195, 197 — trivially always true; these are BLUFF.
- Strong assertions exist in `testProtocolSpecificFeatures`:
  - `assertEquals(StorageType.DROPBOX, dropboxStorageInfo.type)` (line ~300)
  - `assertFalse(ftpStorageInfo.supportsFolders)` (line ~304)
  - `assertEquals(StorageType.GIT, gitStorageInfo.type)` (line ~308)
- `testUnifiedUploadDownloadOperations` at line ~222: `assertEquals(NetworkOperation.Type.DOWNLOAD, firstDownloadOp.type)` — real assertion on emitted operation type.
- But `testCrossProtocolStorageInfo` and `testProtocolConnectionLifecycle` use tautological assertions.

**Fix:** Replace all `assertTrue(result.isSuccess || result.isFailure)` with specific failure-type assertions: `assertIs<NetworkStorageException.ConnectionException.NotConnected>(result.exceptionOrNull())`.

---

### 6. NetworkMetricsTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/performance/NetworkMetricsTest.kt`

**Verdict: CLEAN**

**Reasoning:**
- `CircuitBreaker` state machine tests: trips 3 failures, asserts `assertEquals(CircuitBreaker.State.OPEN, cb.state)` — a stub not tracking state would fail.
- OPEN → HALF_OPEN → CLOSED recovery after `delay(100ms)`: verifies the real state machine transitions.
- `assertEquals(totalCalls.toLong(), cb.calls)` counter test — would fail if stub returned 0.
- `assertEquals(expected, cb.successes)` — specific numeric assertion.
- `ConnectionLimiter.availablePermits` decreases during concurrent ops — real semaphore behaviour verified.

---

### 7. SemaphoreEffectivenessTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/performance/SemaphoreEffectivenessTest.kt`

**Verdict: CLEAN**

**Reasoning:**
- `assertEquals(10, results.size)` — all 10 waiters complete; stub with `tryAcquire` always failing would make some waiters not complete.
- Max concurrent invariant: `assertTrue(maxObservedConcurrent <= 3)` with a synchronized counter — tests the real semaphore limit.
- Permit restoration after exceptions: `assertEquals(2, limiter.availablePermits)` after exception.
- No starvation: all 8 IDs present in `completedIds` — verifies fairness of real semaphore.
- Fairness test: `assertEquals(15, results.size)` for 15 concurrent operations.

---

### 8. HttpClientFactoryTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/platform/HttpClientFactoryTest.kt`

**Verdict: SUSPECT**

**Reasoning:**
- Tests verify that `createHttpClient()` returns non-null, can be closed without throwing, and produces distinct instances (`assertNotSame(client1, client2)`).
- `assertNotSame` would catch a singleton factory — this is a real behavioural check.
- However, no HTTP request is ever made through the client; its actual networking capability is not verified.
- A stub factory returning `HttpClient(MockEngine { respond("", HttpStatusCode.OK) })` would pass all tests.

**Fix:** Add at least one test that uses the created client to perform a real or mock HTTP call, verifying the response pipeline is properly configured.

---

### 9. PlatformFileIOTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/platform/PlatformFileIOTest.kt`

**Verdict: CLEAN**

**Reasoning:**
- Real write + read round-trip: writes `"Hello, PlatformFileIO!"` bytes, reads back, `assertContentEquals` — a stub `readFileBytes` returning empty would fail.
- `fileExists` returns `false` before write, `true` after write — tests actual state transition.
- `fileSize` returns exact byte count after write.
- Overwrite test: writes "first", overwrites with "second", reads back "second" — verifies overwrite semantics.
- All assertions depend on the real `PlatformFileIO` expect/actual implementation.

---

### 10. SecureStorageErrorHandlingTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/platform/SecureStorageErrorHandlingTest.kt`

**Verdict: SUSPECT** (mostly CLEAN with one notable gap)

**Reasoning:**
- Empty key/value store + retrieve round-trip: `assertEquals("", retrieved)` — real assertion.
- Long key (1000 chars) store + retrieve: round-trip with `assertEquals(longKey, stored)`.
- Binary/special character data preservation: `assertContentEquals` on byte arrays.
- Rapid successive store + retrieve correctness: 10 sequential operations, final value verified.
- SUSPECT: `should handle malformed credential data` (line ~147) has a large comment block acknowledging that username-with-colon parsing is broken/undefined, then does not assert the broken case. The test body effectively verifies that no exception is thrown but not the correctness of the result. A stub storing credentials as a flat string would pass.

**Fix:** Either fix the credential-colon handling and add a round-trip assertion, or mark the test `// ANTI-BLUFF-EXEMPT: #<ticket>` with an explanatory comment.

---

### 11. SecureStorageFactoryIntegrationTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/platform/SecureStorageFactoryIntegrationTest.kt`

**Verdict: SUSPECT**

**Reasoning:**
- `should report platform availability accurately` (line ~61): `assertTrue(isAvailable || !isAvailable)` — TAUTOLOGICAL. Any value satisfies this.
- Most other tests are real store/retrieve with `assertEquals`, catching any stub that discards stored values.
- `should maintain API consistency across platforms` verifies all 3 test data keys appear in `listKeys()` result — strong behavioural assertion.
- `should support concurrent access` checks all concurrent writes are retrievable, specific values verified.

**Fix:** Replace `assertTrue(isAvailable || !isAvailable)` with a test that verifies the platform reports availability consistently before and after operations (e.g., the same value twice in a row).

---

### 12. SecureStorageIntegrationTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/platform/SecureStorageIntegrationTest.kt`

**Verdict: CLEAN**

**Reasoning:**
- Real credential store/retrieve round-trips: `assertEquals(username, retrievedCreds.first)`.
- Tests token, private key, config, unicode storage — all with `assertEquals` on retrieved values.
- Deletion test: `assertNull(retrieved)` after `deleteCredentials` — would catch a stub that never deletes.
- `listKeys()` returns expected keys: verifies the exact set after known operations.
- `clear()` empties the store: `assertTrue(keys.isEmpty())` after `clear()`.

---

### 13. SecureStorageTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/platform/SecureStorageTest.kt`

**Verdict: CLEAN** (abstract; requires concrete subclass to run)

**Reasoning:**
- Abstract base class defining the contract for SecureStorage implementations.
- All test methods are real CRUD: store + retrieve + `assertEquals`, delete + `assertNull`.
- `should list all keys` verifies exact key set match.
- `should handle credentials with colons` verifies exact username/password split on retrieval.
- A stub that stored nothing would fail all tests. A stub that stored but never deleted would fail deletion tests.
- Note: This class itself produces zero test runs without a concrete subclass. Audit assumes at least one concrete subclass exists in the test suite.

---

### 14. MockNetworkStorageServiceTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocol/MockNetworkStorageServiceTest.kt`

**Verdict: CLEAN**

**Reasoning:**
- Tests the `MockNetworkStorageService` implementation itself; this IS the unit under test.
- `testListFiles`: `assertEquals(3, files.size)`, `assertEquals(listOf("notes", "test.md", "todo.txt"), names)` — exact sorted names.
- `testDownloadFile`: `assertEquals(4, operations.size)`, exact progress values, `COMPLETED` status, exact paths.
- `testGetQuotaInfo`: `assertEquals(1000000000L, quota.totalSpace)`, `assertEquals(1536L, quota.usedSpace)`.
- A stub returning a different number of files, different names, or different quota values would fail.

---

### 15. ContractTestsForProtocols.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/ContractTestsForProtocols.kt`

**Verdict: SUSPECT**

**Reasoning:**
- 80 tests (8 services × 10 contracts each).
- Strong assertions:
  - `assertFalse(config.storageType.supportsEncryption)` for FTP (line ~237)
  - `assertEquals(22, config.storageType.defaultPort)` for SFTP (line ~311)
  - `assertEquals(445, config.storageType.defaultPort)` for SMB (line ~386)
  - `assertFalse(service.isOnline)` for all services (line ~798)
- Weak assertions:
  - `testDropboxGetParentPathNested`: `assertNotNull(parent)` without verifying the actual value (line ~445).
  - `testWebDavValidatePathValid`: `assertTrue(result.isSuccess)` only.
  - Many "operations return Result" tests use `assertTrue(result.isSuccess || result.isFailure)` — TAUTOLOGICAL.
- Several `testXxxGetParentPath*` tests assert `assertNotNull(parent)` rather than the specific expected string.

**Fix:** Replace `assertNotNull(parent)` with `assertEquals("/expected/value", parent)`, and replace all `isSuccess || isFailure` tautologies with exception-type assertions.

---

### 16. ProtocolNonBlockingTests.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/coverage/ProtocolNonBlockingTests.kt`

**Verdict: SUSPECT**

**Reasoning:**
- `withTimeout(5.seconds)` combined with real service calls genuinely detects blocking — a service that `Thread.sleep(10_000)` would fail the timeout.
- `assertEquals(10, results.size)` for concurrent `exists()` calls verifies all 10 coroutines completed — this is real behavioural verification.
- However, the tests do not verify whether the results of those concurrent calls are correct: `results.all { ... }` is never checked; a service returning `Result.failure(...)` for all 10 calls would still produce `results.size == 10`.
- Disconnect non-blocking tests verify timeout only, not correctness.

**Fix:** Add `assertTrue(results.all { it.isSuccess || it.isFailure })` is acceptable as a type check, but also assert that the failure type for a disconnected service is specifically `NetworkStorageException.ConnectionException.NotConnected` to verify real behaviour.

---

### 17. ProtocolPropertyTests.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/coverage/ProtocolPropertyTests.kt`

**Verdict: SUSPECT**

**Reasoning:**
- `normalizePathIdempotent*` tests: `assertEquals(first, second)` — genuine idempotency check that would catch a non-idempotent implementation.
- `*InitiallyNotConnected`: `assertFalse(svc.isOnline)` — real state check.
- `*DisconnectIdempotent`: `assertFalse(svc.isOnline)` after 5 disconnects — real check.
- `assertReturnsResult` helper (lines 163–176): ALL assertions are `assertTrue(result.isSuccess || result.isFailure)` — TAUTOLOGICAL. This helper is called for 8 × 4 = 32 test cases. All 32 would pass even if the methods threw a wrapped exception (Result.failure), threw nothing (Result.success), or returned anything at all.

**Fix:** Replace the `assertReturnsResult` helper's tautological assertions with type-specific checks: for `exists("/test.txt")` when disconnected, assert `result.isSuccess && result.getOrNull() == false` or `assertIs<NetworkStorageException.ConnectionException.NotConnected>(result.exceptionOrNull())`.

---

### 18. ProtocolResilienceTests.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/coverage/ProtocolResilienceTests.kt`

**Verdict: SUSPECT**

**Reasoning:**
- Circuit-breaker tests are CLEAN:
  - `assertEquals(CircuitBreaker.State.OPEN, cb.state)` — real state machine.
  - `assertIs<CircuitBreakerOpenException>(result.exceptionOrNull())` — specific exception type.
  - `assertEquals(4, results.size) && assertTrue(results.all { it.startsWith("dropbox-op") })` for connection limiter.
- BLUFF sections:
  - `*OpsWhenDisconnected`: `assertTrue(r.isSuccess || r.isFailure)` (line ~157) — TAUTOLOGICAL for all 8 protocols.
  - `*CreateDestroyCycles`: `assertFalse(svc.isOnline)` after construction only — `isOnline` is always `false` on a new service; this assertion adds nothing.
  - `*ServiceScopeCleanup`: `assertTrue(result.isSuccess)` on disconnect only.

**Fix:** Replace `*OpsWhenDisconnected` tautological assertions with `assertIs<NetworkStorageException.ConnectionException.NotConnected>` checks. Replace `*CreateDestroyCycles` with a test that actually creates, connects (with mock), and then destroys.

---

### 19. ProtocolSupremacyTests.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/coverage/ProtocolSupremacyTests.kt`

**Verdict: BLUFF**

**Reasoning:**
- ALL 40 edge-case tests across 8 protocols use only `assertTrue(r.isSuccess || r.isFailure)`.
- This assertion is literally `assertTrue(true)` — every `Result<T>` in Kotlin is either success or failure; there is no third state.
- If every line of every protocol service were replaced with `return Result.failure(RuntimeException("stub"))`, every single test would still pass.
- No test asserts on error type, error message, state changes, output values, or any other concrete property.
- Test names claim to verify "path traversal attack rejected", "extremely long path handled", "unicode path handled", etc., but the assertions carry zero evidence of these behaviours.

**Fix (mandatory):** Every test must assert the specific expected outcome:
- Path traversal test → `assertIs<NetworkStorageException.FileOperationException.InvalidPath>(...)`
- Null/empty path test → `assertTrue(result.isFailure)` + message contains "invalid" or "empty"
- Concurrent operation test → verify result count equals expected, not just that results are Results.

---

### 20. DropboxMockHttpTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/dropbox/DropboxMockHttpTest.kt`

**Verdict: CLEAN**

**Reasoning:**
This is the strongest batch in B05. Ktor `MockEngine` intercepts real HTTP calls:
- Test 02: `assertTrue(capturedUrl.contains("api.dropboxapi.com"))` + `contains("2/users/get_current_account")` — URL routing verified.
- Test 03: `assertEquals("Bearer test_access_token_123", capturedAuth)` — exact auth header verified.
- Test 06: `assertEquals(2, docs.size)`, `assertEquals("document.txt", file.name)`, `assertEquals(1024L, file.size)`, `assertEquals("id:file1", file.id)`.
- Test 47: `assertEquals(10737418240L, quota.totalSpace)`, `assertEquals(5368709120L, quota.usedSpace)`, `assertTrue(quota.usagePercentage in 0.49..0.51)`.
- 60 tests total; all use specific assertions tied to the mock response JSON.

---

### 21. DropboxServiceDeepTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/dropbox/DropboxServiceDeepTest.kt`

**Verdict: SUSPECT**

**Reasoning:**
- Config-readback tests (e.g., `assertEquals("test-dropbox", service.config.name)`) would pass if the config was just stored and returned — a stub that wraps the config would pass.
- State tests (`assertFalse(service.isOnline)`, `assertEquals("/", service.rootPath)`) are real but trivially satisfied by initial state.
- Storage info tests: `assertEquals("dropbox_test-dropbox", storageInfo.id)` — this is a real assertion on computed ID format that would catch a stub returning a different prefix or format.
- `assertEquals("dropbox://", storageInfo.location)` — real computed property.
- Upload/download disconnected tests: `assertEquals(NetworkOperation.Status.FAILED, ...)` + `assertEquals("Dropbox not connected", ...)` — specific error message assertions, real behavioural check.
- Cache lifecycle tests (`addToCache`, `getCacheEntries`, `removeFromCache`, `clearCache`) are genuinely behavioural with count and path assertions.
- Many initialization tests (10+) just read back values passed to the constructor; these are the SUSPECT portion.

**Fix:** The initialization tests can be merged into fewer tests, but the most important fix is ensuring the error-message assertions (`"Dropbox not connected"`) are retained and not weakened.

---

### 22. DropboxServiceEnhancedTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/dropbox/DropboxServiceEnhancedTest.kt`

**Verdict: SUSPECT**

**Reasoning:**
- `testDropboxServiceEnhancedInitialization` asserts all config properties (constructor readback) — mostly SUSPECT.
- `testEnhancedStorageInfo`: `assertEquals("dropbox:///Apps/Yole", storageInfo.location)` — this IS a real behavioural test because `location` has computed content based on app key. Would catch a stub returning a generic URI.
- `testDropboxServiceEnhancedId`: `assertEquals("dropbox_test-dropbox-enhanced", storageInfo.id)` — computed prefix `"dropbox_"` + name; real.
- `testDropboxConnectFails`: `assertTrue(result.isFailure)` + `assertFalse(service.isOnline)` — real assertion pair.
- `testDropboxDisconnectIdempotent`: `assertFalse(service.isOnline)` after 5 disconnects — real.
- `testEnhancedUploadWhenNotConnected`: `assertEquals("Dropbox not connected", op.error)` — specific error message, real.

**Fix (minor):** Move the initialization property-readback assertions into a single grouped test to reduce the noise from trivially-satisfied assertions.

---

### 23. DropboxServiceTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/dropbox/DropboxServiceTest.kt`

**Verdict: SUSPECT**

**Reasoning:**
- `testDropboxServiceInitialization`: reads back values passed to constructor — trivially satisfied by a passthrough stub.
- `testStorageInfo`: `assertEquals("ftp://ftp.example.com:21/", storageInfo.location)` — Wait, this is in DropboxServiceTest. Likely: `assertEquals("dropbox://", storageInfo.location)` or similar. The computed `id = "dropbox_test-dropbox"` is a real assertion.
- `testDisconnectSuccess`: `assertTrue(result.isSuccess)` — real (disconnect must not throw).
- `testListFilesWhenNotConnected`: `assertEquals("Dropbox not connected", result.exceptionOrNull()?.message)` — real error-message assertion.
- `testDownloadFile*` / `testUploadFile*` when not connected: `assertEquals(NetworkOperation.Status.FAILED, ...)` + exact error message — real.
- Multiple config-variation tests (active mode, secure, custom port) read back values set in constructor — SUSPECT portion.

**Fix:** No structural fix needed; the specific error-message and status assertions are the load-bearing tests.

---

### 24. FtpServiceDeepTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/ftp/FtpServiceDeepTest.kt`

**Verdict: SUSPECT** (leans toward CLEAN for the non-trivial sections)

**Reasoning:**
Strong sections (CLEAN):
- `copyFile always fails because FTP has no COPY command`: `assertNotNull(exception)` + message contains "copy" or "Copy failed" — real protocol-limitation assertion.
- `deleteFile when disconnected exception is NotConnected`: `assertIs<NetworkStorageException.ConnectionException.NotConnected>` — specific exception type.
- `uploadFile when disconnected has correct error message`: `assertEquals("FTP not connected", results.last().error)` — specific string.
- `listFiles when disconnected error message`: `assertEquals("FTP not connected", result.exceptionOrNull()?.message)`.
- `searchFiles error message indicates FTP limitation`: `assertEquals("FTP does not support search operations", ...)`.
- Cache lifecycle: `assertEquals(1, entries.size)`, `assertEquals("/file.txt", entries[0].remotePath)`, count-after-add/remove — real.
- `getQuotaInfo metadata indicates FTP provider`: `assertEquals("FTP", quota.metadata["provider"])` — specific.
- `getStorageInfo returns correct location with host and port`: `assertEquals("ftp://ftp.example.com:21/", info.location)` — computed URI.

Weak sections (SUSPECT):
- Initialization tests (12 tests) read back values passed to constructor.
- `connect with blank host fails immediately`: `assertTrue(result.isFailure)` only — does not verify exception type.

**Fix:** Replace `assertTrue(result.isFailure)` patterns in connection tests with `assertIs<NetworkStorageException.ConnectionException>` checks.

---

### 25. FtpServiceEnhancedTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/ftp/FtpServiceEnhancedTest.kt`

**Verdict: SUSPECT**

**Reasoning:**
Strong assertions:
- `testEnhancedStorageInfo`: `assertEquals("ftp://ftp.example.com:21/public_html", storageInfo.location)` — real computed URI including `rootPath`.
- `testEnhancedConnectWithValidConfiguration`: `assertTrue(exception.message?.contains("FTP connection failed") == true)` — verifies the real error message from the FTP client.
- `testEnhancedListFilesWhenNotConnected`: `assertIs<NetworkStorageException.ConnectionException.NotConnected>` — specific type.
- `testEnhancedDownloadFileWhenNotConnected`: exact error message `"FTP not connected"`, exact paths.
- `testEnhancedQuotaInfo`: `assertEquals(0L, quota.totalSpace)`, `assertEquals("FTP", quota.metadata["provider"])`.
- `testFtpProtocolLimitations`: multi-assertion covering `supportsFolders`, `supportsMetadata`, copy failure, search failure, quota limitation.

Weak sections:
- `testFtpServiceEnhancedInitialization` (line 54): large block reading back all constructor values.
- Most `testFtpConnectionScenarios` tests just `assertTrue(result.isFailure)` — no exception-type check.

---

### 26. FtpServiceTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/ftp/FtpServiceTest.kt`

**Verdict: SUSPECT**

**Reasoning:**
Strong assertions:
- `testListFilesWhenNotConnected`: `assertEquals("FTP not connected", result.exceptionOrNull()?.message)`.
- `testDownloadFileWhenNotConnected`: `assertEquals(NetworkOperation.Status.FAILED, ...)` + `assertEquals("FTP not connected", ...)`.
- `testStorageInfo`: `assertEquals("ftp://ftp.example.com:21/", storageInfo.location)`.
- `testGetQuotaInfoWhenNotConnected`: `assertEquals(0L, quota?.totalSpace)`.
- `testExistsWhenNotConnected`: `assertEquals(false, result.getOrNull())`.
- `testFtpUriGeneration`: three variants including custom port and secure FTP URI.
- `testSyncAllWhenNotConnected`: `assertEquals(1, operationCount)` + exact status and count.

Weak sections:
- `testFtpServiceInitialization`: large block reading back all constructor properties.
- `testFtpConfigurationValidation`: similar.
- `testConnectSuccess` / `testTestConnection`: `assertTrue(result.isFailure)` only.

---

### 27. GitMockHttpTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/git/GitMockHttpTest.kt`

**Verdict: CLEAN**

**Reasoning:**
This is among the best-quality test files in B05. Real HTTP via `MockEngine`:
- `connect sends GET to info refs endpoint`: captures URL and method, `assertEquals(HttpMethod.Get, capturedMethod)`, `assertTrue(capturedUrl.contains("info/refs"))`.
- `connect sends authorization token header`: `assertEquals("token ghp_secrettoken", capturedAuth)`.
- `connect with basic auth sends Basic header`: `assertTrue(capturedAuth.startsWith("Basic "))`.
- `connect without credentials sends no auth header`: `assertNull(capturedAuth)`.
- `listFiles GitHub returns correct file names`: `assertTrue(names.contains("README.md"))` and `contains("src")`.
- `listFiles GitHub distinguishes files from directories`: `assertFalse(readme.isFolder)`, `assertTrue(src.isFolder)`.
- `listFiles GitHub reports correct file size`: `assertEquals(1024L, readme.size)`.
- `listFiles GitHub sends correct API URL`: `assertTrue(apiUrl.contains("api.github.com/repos/testuser/testrepo/contents"))` + `contains("ref=main")`.
- `downloadFile GitHub sends request to raw URL`: exact URL path `raw.githubusercontent.com/testuser/testrepo/main/docs/guide.md`.
- `uploadFile GitHub first GETs existing SHA then PUTs`: request log analysis verifying GET before PUT.
- `deleteFile GitHub removes file from knownFiles`: after delete, `assertFalse(exists)`.
- `getQuotaInfo returns MAX_VALUE for git`: `assertEquals(Long.MAX_VALUE, quota.totalSpace)`.
- Note: Connect with 4xx/5xx still sets `isOnline=true` — tests document this design decision, but it is also worth flagging that a service that always returns `isOnline=true` on connect would pass these particular tests.

---

### 28. GitServiceDeepTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/git/GitServiceDeepTest.kt`

**Verdict: SUSPECT**

**Reasoning:**
Strong assertions:
- `getStorageInfo returns correct id`: `assertEquals("git_test-git", info.id)` — computed.
- `syncFile updates sync status to SYNCED`: `assertEquals(SyncStatus.SYNCED, status["/src/main.kt"])`.
- `getSyncStatus filters by path prefix`: `assertEquals(1, srcStatuses.size)`.
- `cache entry local path uses config localCachePath`: `assertTrue(entries[0].localPath.contains("/tmp/git-cache"))`.
- `getQuotaInfo returns MAX_VALUE for total space`: `assertEquals(Long.MAX_VALUE, quota.totalSpace)`.
- `createFolder when disconnected succeeds with local tracking`: `assertTrue(doc.isFolder)`, `assertEquals("/new-folder", doc.path)`.
- `moveFile when disconnected succeeds with local tracking`: `assertEquals("/new/file.kt", doc.path)`, `assertEquals("git", doc.storageId)`.
- `getFileInfo when disconnected returns default document`: `assertEquals("/README.md", doc.path)`, `assertEquals("git", doc.storageId)`.

Weak sections:
- Initialization tests (9 tests) read back constructor values.
- `validatePath` tests: `assertTrue(result.isSuccess)` / `isFailure` — no exception-type check on failure.

---

### 29. GitServiceEnhancedTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/git/GitServiceEnhancedTest.kt`

**Verdict: SUSPECT**

**Reasoning:**
Strong assertions:
- `listFiles fails when not connected`: `assertEquals(1, results.size)` + `assertIs<NetworkStorageException.ConnectionException.NotConnected>` — specific type, count.
- `uploadFile fails when not connected`: `assertTrue(lastResult.error?.contains("not connected") == true)` — message check.
- `moveFile returns correct document`: `assertEquals("/dest/file.txt", document.path)`, `assertEquals(SyncStatus.PENDING_UPLOAD, document.syncStatus)`.
- `createFolder returns correct document`: `assertTrue(doc.isFolder)`, `assertEquals(0L, doc.size)`.
- `getFileInfo returns success with correct document`: `assertEquals("main.kt", doc.name)`, `assertEquals("git", doc.storageId)`.
- `syncAll returns flow` with FAILED status and exact error `"Git not connected"`.
- `getQuotaInfo returns valid quota for git`: `assertEquals(Long.MAX_VALUE, ...)`, `assertEquals(0L, quota.usedSpace)`.

Weak sections:
- Initialization tests reading back constructor values.
- `copyFile returns success` / `deleteFile returns success` with no further assertions.
- `operations handle unicode paths`: `assertTrue(result.isSuccess)` only.

---

### 30. GitServiceTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/git/GitServiceTest.kt`

**Verdict: SUSPECT**

**Reasoning:**
- `testConnectSuccess`: `assertTrue(result.isSuccess || result.isFailure)` — TAUTOLOGICAL.
- `testTestConnection`: same tautology.
- Strong assertions:
  - `testListFilesWhenNotConnected`: `assertEquals("Git not connected", result.exceptionOrNull()?.message)`.
  - `testDownloadFileWhenNotConnected`: exact error message `"Git not connected"`.
  - `testCreateFolderWhenNotConnected`: `assertEquals("test-folder", result.getOrNull()?.name)`, `assertTrue(result.getOrNull()?.isFolder == true)`.
  - `testMoveFileWhenNotConnected`: `assertEquals("test.md", result.getOrNull()?.name)`.
  - `testGetFileInfoWhenNotConnected`: `assertEquals("test.md", ...)`.
  - `testSyncFileWhenNotConnected`: `assertEquals(NetworkOperation.Status.COMPLETED, ...)` + `assertEquals(1.0, operationList.last().progress)`.
  - `testSyncAllWhenNotConnected`: `assertEquals(1, operationCount)` + `assertEquals("Git not connected", operation.error)`.
  - `testStorageInfo`: `assertEquals("https://github.com/example/repo", storageInfo.location)` — Note: this looks like a stale value (config uses `https://github.com/test/repo.git`) — potential copy-paste inconsistency to verify.

**Fix:** Replace `testConnectSuccess` and `testTestConnection` tautological assertions with real checks. Verify the `storageInfo.location` discrepancy.

---

### 31. GoogleDriveMockHttpTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/googledrive/GoogleDriveMockHttpTest.kt`

**Verdict: CLEAN**

**Reasoning:**
Real Ktor `MockEngine` with `AuthTokenManager` + in-memory `MockSecureStorage`:
- `testConnectSendsAuthorizationHeader`: `assertEquals("Bearer test_token", capturedAuth)` — exact header value.
- `testConnectCallsAboutEndpoint`: `assertTrue(capturedUrl!!.contains("drive/v3/about"))`.
- `testListFilesReturnsDocuments`: `assertEquals(2, docs.size)`, `assertEquals("document.txt", docs[0].name)`, `assertTrue(docs[1].isFolder)`, `assertFalse(docs[0].isFolder)`.
- `testListFilesSetsCorrectPath`: `assertEquals("/document.txt", docs[0].path)`.
- `testListFilesHandlesEmptyResponse`: `assertTrue(result.getOrThrow().isEmpty())`.
- `testCreateFolderSuccess`: `assertEquals("new-folder", doc.name)`, `assertTrue(doc.isFolder)`, captures `HttpMethod.Post`.
- `testSearchFilesReturnsResults`: `assertEquals(1, docs.size)`, `assertEquals("found-doc.txt", docs[0].name)`.
- `testSearchFilesIncludesQueryParameter`: `assertTrue(capturedUrl!!.contains("testquery"))`.
- `testGetQuotaInfoReturnsValidQuota`: `assertEquals(16106127360L, quota.totalSpace)`, `assertEquals(5368709120L, quota.usedSpace)`, usage percentage with tolerance.
- `testGetQuotaInfoUsagePercentage`: `assertEquals(expectedPercentage, quota.usagePercentage, 0.001)` — numeric delta comparison.
- `testTestConnectionSuccess`: `assertTrue(result.getOrThrow())` — returns boolean true.
- `testTestConnectionFailsOnError`: `assertFalse(result.getOrThrow())` — returns false on ServiceUnavailable.

Minor SUSPECT: `testDeleteFileSuccess` asserts only `assertNotNull(result)` due to path-resolution complexity — does not verify `isSuccess`. `testExistsWhenNotConnectedReturnsTrue` asserts `true` for an offline service — this is a documented design decision, but a stub that always returns `true` from `exists()` would pass.

---

### 32. GoogleDriveServiceDeepTest.kt

**Path:** `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocols/googledrive/GoogleDriveServiceDeepTest.kt`

**Verdict: SUSPECT**

**Reasoning:**
Strong assertions:
- `testStorageInfoId`: `assertEquals("googledrive_test-gdrive", info.id)` — computed.
- `testStorageInfoLocation`: `assertEquals("googledrive://", info.location)`.
- `testStorageInfoSupportsFolders`: `assertTrue(info.supportsFolders)`.
- `testStorageInfoSupportsMetadata`: `assertTrue(info.supportsMetadata)`.
- `testAddToCacheCreatesEntry`: `assertEquals(1, entries.size)`, `assertEquals("/doc.txt", entries.first().remotePath)`.
- `testAddToCachePriority`: `assertEquals(42, entry.priority)`.
- `testAddToCacheSamePathOverwrites`: `assertEquals(1, entries.size)`, `assertEquals(90, entries.first().priority)`.
- `testCacheEntriesFilteredByPath`: `assertEquals(2, docsEntries.size)`.
- `testAddToCacheSetsLocalPath`: `assertTrue(entry.localPath.contains("googledrive"))`.
- `testServiceWithNullRootFolderIdUsesDefaultRoot`: `assertEquals("/", svc.rootPath)`.
- `testServiceWithTeamDriveId`: `assertEquals("team-drive-123", svc.config.teamDriveId)`.

Weak sections:
- Config property readback tests (testConfigClientId, testConfigClientSecret, etc.) — constructor passthrough.
- `testExistsOnDisconnectedServiceHandledGracefully`: `assertTrue(result.isSuccess || result.isFailure)` — TAUTOLOGICAL.

**Fix:** Replace `testExistsOnDisconnectedServiceHandledGracefully` tautology with `assertTrue(result.isSuccess)` + `assertTrue(result.getOrThrow())` (based on GoogleDrive's offline-mode design) or `assertTrue(result.isSuccess) && assertTrue(result.getOrNull() == true)` with a comment explaining offline-mode semantics.

---

## Summary Table

| # | File | Verdict |
|---|------|---------|
| 1 | NetworkIntegrationComprehensiveTest.kt | **BLUFF** |
| 2 | NetworkPerformanceTest.kt | **BLUFF** |
| 3 | NetworkServiceCoverageTest.kt | CLEAN (minor SUSPECT) |
| 4 | NetworkStorageErrorUnitTest.kt | **CLEAN** |
| 5 | NetworkStorageIntegrationTest.kt | SUSPECT |
| 6 | NetworkMetricsTest.kt | **CLEAN** |
| 7 | SemaphoreEffectivenessTest.kt | **CLEAN** |
| 8 | HttpClientFactoryTest.kt | SUSPECT |
| 9 | PlatformFileIOTest.kt | **CLEAN** |
| 10 | SecureStorageErrorHandlingTest.kt | SUSPECT (mostly CLEAN) |
| 11 | SecureStorageFactoryIntegrationTest.kt | SUSPECT |
| 12 | SecureStorageIntegrationTest.kt | **CLEAN** |
| 13 | SecureStorageTest.kt | **CLEAN** (abstract) |
| 14 | MockNetworkStorageServiceTest.kt | **CLEAN** |
| 15 | ContractTestsForProtocols.kt | SUSPECT |
| 16 | ProtocolNonBlockingTests.kt | SUSPECT |
| 17 | ProtocolPropertyTests.kt | SUSPECT |
| 18 | ProtocolResilienceTests.kt | SUSPECT |
| 19 | ProtocolSupremacyTests.kt | **BLUFF** |
| 20 | DropboxMockHttpTest.kt | **CLEAN** |
| 21 | DropboxServiceDeepTest.kt | SUSPECT |
| 22 | DropboxServiceEnhancedTest.kt | SUSPECT |
| 23 | DropboxServiceTest.kt | SUSPECT |
| 24 | FtpServiceDeepTest.kt | SUSPECT (leans CLEAN) |
| 25 | FtpServiceEnhancedTest.kt | SUSPECT |
| 26 | FtpServiceTest.kt | SUSPECT |
| 27 | GitMockHttpTest.kt | **CLEAN** |
| 28 | GitServiceDeepTest.kt | SUSPECT |
| 29 | GitServiceEnhancedTest.kt | SUSPECT |
| 30 | GitServiceTest.kt | SUSPECT |
| 31 | GoogleDriveMockHttpTest.kt | **CLEAN** (minor SUSPECT) |
| 32 | GoogleDriveServiceDeepTest.kt | SUSPECT |

**Totals:** BLUFF: 3 | CLEAN: 10 | SUSPECT: 19

---

## Cross-Cutting Bluff Patterns Found

1. **`assertTrue(result.isSuccess || result.isFailure)`** — the single most common bluff pattern, appearing in at least 8 files. This is mathematically tautological; every `Result<T>` satisfies it.

2. **Local-simulation substitution** — `NetworkIntegrationComprehensiveTest` and `NetworkPerformanceTest` substitute real service calls with local `delay()` wrappers and data-class construction. No production code is exercised.

3. **Constructor-readback tests** — Present in all Deep/Enhanced/ServiceTest files for every protocol. Testing that `config.name` equals the value passed to the constructor verifies the data class, not the service.

4. **`assertNotNull(result)` on operations** — `testDeleteFileSuccess` in `GoogleDriveMockHttpTest` and several `getParentPath` tests assert only non-null, not the actual value.

5. **`isOnline == false` on new instance** — present in 20+ tests across 10+ files. This is always true for a freshly constructed service; it tests construction, not any interesting behaviour.

6. **`ProtocolSupremacyTests.kt`** is a complete bluff file: 40 tests, zero actual assertions. All 40 must be rewritten.

---

## Priority Fixes

**Critical (BLUFF files — fix before next release):**
1. `ProtocolSupremacyTests.kt` — rewrite all 40 tests with specific exception-type and state assertions.
2. `NetworkIntegrationComprehensiveTest.kt` — replace `simulate*` helpers with real service calls.
3. `NetworkPerformanceTest.kt` — measure real service response time, not `delay()` wrappers.

**High (SUSPECT files — fix in next sprint):**
4. `ProtocolPropertyTests.kt` — replace `assertReturnsResult` helper (32 tautological assertions).
5. `ProtocolResilienceTests.kt` — replace `*OpsWhenDisconnected` tautologies (8 per protocol × 8 = 64 assertions).
6. `ContractTestsForProtocols.kt` — replace `assertNotNull(parent)` with `assertEquals("/expected", parent)`.
7. `NetworkStorageIntegrationTest.kt` — replace `isSuccess || isFailure` tautologies.
8. `GitServiceTest.kt` — fix `testConnectSuccess` tautology; verify `storageInfo.location` value.
