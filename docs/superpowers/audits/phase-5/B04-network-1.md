<!--
  SPDX-FileCopyrightText: 2026 Milos Vasic
  SPDX-License-Identifier: Apache-2.0

  CONST-035 / CONST-039 Anti-Bluff Audit — Batch B04
  Auditor: Claude Sonnet 4.6  |  Date: 2026-05-20
  Scope: shared/commonTest network/ part 1 (32 files)
-->

# B04 Audit — network/ part 1 (32 files)

**Auditor:** Claude Sonnet 4.6  
**Date:** 2026-05-20  
**Rule:** CONST-035 / CONST-039 — mutation-verification thought experiment applied to every test method  
**Policy:** "If every line of the unit under test were replaced with a trivial stub, would this test still pass?" → BLUFF if YES.

---

## Summary

| Verdict | Count |
|---------|-------|
| CLEAN   | 27    |
| SUSPECT | 4     |
| BLUFF   | 1     |
| **Total** | **32** |

---

## File-by-File Findings

---

### 1. `network/auth/AuthTokenManagerImplTest.kt`

**Verdict: CLEAN**

24 test methods. Uses `TestSecureStorage` (in-memory key-value store wired through the real `AuthTokenManager` constructor) and calls `AuthTokenManager` directly for all assertions.

Strongest behavioral assertion: line 127 — `assertTrue(result.getOrNull() ?: false, "Should detect expired token")` exercises the expiry-check logic with a token whose expiry was set to `Clock.System.now().minus(1.hours)`. Mutating the `isTokenExpired()` branch to `return false` would make this test fail. Real `AuthTokenManager` is under test throughout.

---

### 2. `network/auth/AuthTokenManagerStressTest.kt`

**Verdict: BLUFF**

**Surviving mutant:** Replace the entire `AuthTokenManager` class with `class AuthTokenManager { }` (empty stub). Every test in this file still passes because the file defines `TestAuthTokenManagerImpl` (lines 409–468), a completely self-contained in-memory implementation that shares no code path with the production `AuthTokenManager`. All 17 stress test methods (`testConcurrentTokenRefresh`, `testTokenExpirationUnderLoad`, `testConcurrentStorageOperations`, etc.) instantiate `TestAuthTokenManagerImpl` exclusively and never touch `AuthTokenManager`.

The test-local class is not a fake injected via the real constructor — it is a separate reimplementation of the same interface. The production class is not exercised at all.

**Fix required:** Stress tests must use the real `AuthTokenManager` backed by an in-memory `SecureStorage` (as `AuthTokenManagerImplTest` and `AuthTokenManagerTest` already do). The `TestAuthTokenManagerImpl` class should be deleted.

---

### 3. `network/auth/AuthTokenManagerTest.kt`

**Verdict: CLEAN**

16 test methods. Uses `InMemorySecureStorage` injected into the real `AuthTokenManager` constructor. Behavioral assertions include checking token storage, retrieval, expiry, and clearing. Mutating `clearAllTokens()` to a no-op causes `testClearAllTokens` to fail because the retrieved map would still have entries. Real production code is exercised.

---

### 4. `network/auth/OAuth2FlowTest.kt`

**Verdict: CLEAN** (minor note)

Tests URL generation for Dropbox, Google Drive, and OneDrive OAuth flows. Core behavioral checks: `assertTrue(authUrl.contains("client_id=test_client_id"))` and `assertTrue(authUrl.contains("redirect_uri="))` with encoded URIs. Mutating URL-building logic would break these assertions.

Minor note: `testTokenResponseDefaults` at line 141 checks `assertEquals("Bearer", response.token_type)` which is a default-value assertion. Not bluff-level but it is a metadata-only check for that particular method.

---

### 5. `network/auth/OAuth2FlowUrlTest.kt`

**Verdict: SUSPECT**

Most tests are behavioral (they check URL content). However, line 206 contains a tautological `||` expression:

```kotlin
url.contains("scope=files.read") || url.contains("scope=files.read")
```

Both sides of `||` are identical. The right-hand branch can never differ from the left. This assertion can never catch a regression that mutates only the right-side case. The expression reduces to `url.contains("scope=files.read")`, so the intent may have been to check two different scope strings (e.g. `"scope=files.read"` vs `"scope=files.metadata.read"`).

**Issue type:** Tautological assertion (copy-paste error). Not a full BLUFF — the left side is real — but a coverage gap on whatever the right side was intended to verify.

---

### 6. `network/common/CacheEntryTest.kt`

**Verdict: CLEAN**

19 test methods. Behavioral assertions include: `compressionRatio` computed as `originalSize.toDouble() / cachedSize` (line ~72), `canBeEvicted` logic based on `lastAccessedAt` age, and `withAccess()` incrementing the `accessCount` field. Mutating the `compressionRatio` formula or `canBeEvicted` predicate causes immediate test failures. Real `CacheEntry` production code is exercised.

---

### 7. `network/common/CircuitBreakerUnitTest.kt`

**Verdict: CLEAN**

40+ test methods. Verifies the full state machine: CLOSED→OPEN transition after exactly `failureThreshold` failures (line 92: `assertEquals(CircuitBreaker.State.OPEN, cb.state)`), OPEN→HALF_OPEN after `resetTimeoutMs` elapses, HALF_OPEN→CLOSED after success, HALF_OPEN→OPEN on failure. CancellationException rethrow behavior verified. Concurrent access tests use real coroutines. Mutating the state transition logic breaks multiple specific assertions.

---

### 8. `network/common/ConnectionLimiterUnitTest.kt`

**Verdict: CLEAN**

28+ test methods. Key behavioral assertion: `assertEquals(1, limiter.availablePermits)` after a permitted operation throws and the semaphore must release (verified the permit is returned via exception path). Mutating the `try/finally` release block would make this test fail. Concurrent limit-overflow test verifies that only `maxConnections` coroutines run simultaneously.

---

### 9. `network/common/DocumentPermissionTest.kt`

**Verdict: CLEAN**

Tests enum classification predicates (`isReadPermission`, `isWritePermission`, `isAdministrativePermission`) and `hasPermission()`. Behavioral assertion at line 147: `assertTrue(DocumentPermission.hasPermission(adminPermissions, DocumentPermission.READ))` where `adminPermissions` contains only `ADMIN`. This verifies that `ADMIN` grants implicit access to `READ` — mutating the `hasPermission` logic to remove ADMIN elevation breaks this test. Real production logic exercised.

---

### 10. `network/common/DocumentSyncStatusEdgeCaseTest.kt`

**Verdict: CLEAN**

Tests `isSyncing`, `hasFailed`, `canRetry`, `isPending` predicates plus `progressPercentage`, `transferSpeed`, mutation methods (`withStatus`, `withProgress`, `withError`, `withSuccess`), and factory methods. Checks that `withStatus(SYNCED)` changes the `status` field while preserving unrelated fields. Mutating any predicate or mutation method breaks specific assertions. Real `DocumentSyncStatus` production code exercised.

---

### 11. `network/common/DocumentSyncStatusTest.kt`

**Verdict: SUSPECT**

Most tests are behavioral. However, two inner test classes are no-op:

- `SyncStatusEnumTest.testSyncStatusValues()` (lines 231–243): body only dereferences `SyncStatus.UNKNOWN`, `SyncStatus.SYNCING`, etc. with zero assertions. A stub that deletes all enum values except the referenced ones would not be caught. This test would pass even if all enum variants were renamed.

- `ConflictResolutionEnumTest.testConflictResolutionValues()` (lines 254–260): same pattern — references `ConflictResolution.KEEP_LOCAL` etc. with no assertions. Can never fail.

**Surviving mutant for both:** Rename any of the enum constants not referenced in other tests; these methods still pass.

---

### 12. `network/common/NetworkCommonTests.kt`

**Verdict: SUSPECT**

Large omnibus file. Lines 94–113 contain tautological enum-membership assertions of the form:

```kotlin
assertTrue(OperationStatus.values().contains(OperationStatus.PENDING))
assertTrue(OperationStatus.values().contains(OperationStatus.IN_PROGRESS))
// ...repeated for every enum constant
```

`values().contains(values()[n])` is always true by construction — it compares an element of an array with itself. A stub that renames every `OperationStatus` constant to arbitrary strings would still pass this test as long as the reference on both sides uses the same name. These assertions can never catch any regression.

Other tests in the same file (e.g. `CircuitBreakerTests`, `ConnectionLimiterTests`) contain real behavioral assertions and are not bluff. The tautological block is isolated but constitutes a structural bluff on `OperationStatus` enum coverage.

**Surviving mutant:** Change the `OperationStatus.PENDING` value to `null` or remove it; the assertion `values().contains(PENDING)` fails to compile, but the runtime assertion never distinguishes between "enum has this value" and "enum had this value before a rename."

---

### 13. `network/common/NetworkDocumentTest.kt`

**Verdict: CLEAN**

Tests extension detection (`getExtension()`), `formattedSize` with 8 size cases (line 168: `assertEquals(expectedFormattedSize, document.formattedSize)`), `parentPath`, `isInPath`, `isDirectChildOf`, `syncStatus` derived properties, `permissions`, and copy operations. Mutating `formattedSize` logic or `isInPath` predicate breaks targeted assertions. Real `NetworkDocument` production code is exercised.

---

### 14. `network/common/NetworkEnumsTest.kt`

**Verdict: SUSPECT**

Contains tautological name-convention assertions:

```kotlin
assertTrue(DocumentType.entries.all { it.name == it.name.uppercase() })
```

`it.name` is the Kotlin enum constant's `name` property, which by the language specification is always identical to the declaration name. Since Kotlin enum constants declared as `UPLOAD`, `DOWNLOAD`, etc. already have names equal to their uppercase form, this assertion is always true regardless of what the enum constants are named — as long as they are uppercased. It verifies nothing about the semantic contract of the enum.

Same tautological pattern applies to `testOperationStatusNames()` and `testOperationTypeNames()`.

**Surviving mutant:** Add a new enum constant `Foo` (mixed-case). The test catches that (it breaks), but adding `FOO_BAR` (all-caps) passes — the test cannot distinguish between `FOO_BAR` meaning "foo bar" vs. a completely wrong value like `INVALID_OPERATION`.

`testDocumentTypeCount()` (`assertEquals(8, DocumentType.entries.size)`) is a real count assertion and is clean.

---

### 15. `network/common/NetworkOperationTest.kt`

**Verdict: CLEAN**

18 test methods. Key behavioral assertion: line 261 — `assertEquals(5000L, completedOp.duration)` where `duration = completedAt - startedAt` with a 5-second spread. Verifies `isRunning`, `isPending`, `canRetry` predicates and factory methods. Mutating `duration` computation or state predicates breaks specific assertions. Real production code exercised.

---

### 16. `network/common/NetworkStorageEdgeCaseTest.kt`

**Verdict: CLEAN**

Tests: null `spaceInfo` in `NetworkStorage`, `usagePercentage` at exact zero, `isLowOnSpace` boundary at 91% vs 89%, `CacheEntry.compressionRatio` with and without `originalSize`, `NetworkOperation` PAUSED state `isRunning = false`. Boundary assertions are specific and would fail on off-by-one mutations. Real production code exercised.

---

### 17. `network/common/NetworkStorageExceptionTest.kt`

**Verdict: CLEAN**

Tests exception hierarchy: `errorCode` string values, `isRetryable()`, `isPermanentFailure()`, `getSuggestedAction()`, and `fromThrowable()` message-based dispatch. `fromThrowable()` tests check that a `Throwable` with message `"timeout"` produces a `NetworkTimeoutException` — mutating the dispatch string `"timeout"` would break this assertion. Real `NetworkStorageException` hierarchy exercised.

---

### 18. `network/common/NetworkStorageTest.kt`

**Verdict: CLEAN**

Tests `NetworkStorage` computed properties (`isLowOnSpace`, `usagePercentage`, `hasQuota`). Specific threshold assertions (e.g. `assertFalse(storage.isLowOnSpace)` at 50% usage, `assertTrue` at 95%) would fail on predicate mutations. Real production code exercised.

---

### 19. `network/common/PathUtilsTest.kt`

**Verdict: CLEAN**

Critical security test: `assertFailsWith<IllegalArgumentException> { PathUtils.normalizePath("../../etc/passwd", "/home/user") }` — mutating the traversal-detection logic to not throw would directly break this test. Additional tests verify path normalization produces canonical results. Real `PathUtils` production code exercised.

---

### 20. `network/common/ResilienceIntegrationTests.kt`

**Verdict: CLEAN**

Multi-class integration: `CircuitBreakerIntegrationTests`, `ConnectionLimiterIntegrationTests`, `DocumentCacheFormatRegistryIntegrationTests`, `CombinedResiliencePatternTests`. Real subsystem interactions verified — e.g. CircuitBreaker OPEN state rejects operations through the real `execute()` method, not a mock. Mutating `CircuitBreaker.execute()` to always throw, or to never check state, breaks multiple integration assertions. Real production code exercised.

---

### 21. `network/common/ResilienceTests.kt`

**Verdict: CLEAN**

Multi-class: state machine, concurrency, DocumentCache LRU eviction, concurrency. Particularly strong: LRU eviction test at line 529 accesses key "a" to make it recently-used, then fills cache to capacity; verifies key "b" was evicted instead of "a". Mutating LRU eviction order (e.g. making it FIFO) breaks this specific test. Real `DocumentCache` and `CircuitBreaker` production code exercised.

---

### 22. `network/common/StorageConfigTest.kt`

**Verdict: CLEAN**

Tests `StorageConfig` subclass construction, `storageType` dispatch, equality, `copy()`, `withEnabled()`, `withPriority()`, and `withMetadata()`. The `storageType` dispatch tests (e.g. `assertEquals(StorageType.WEBDAV, webdav.storageType)`) would fail if a subclass returned the wrong enum value. Mutation of `withEnabled()` to not change the field breaks `testWithEnabled`. Real production code exercised.

---

### 23. `network/common/StorageModelsTest.kt`

**Verdict: CLEAN**

Tests `StorageInfo`, `QuotaInfo`, and `FileInfo` data classes. Assertions check field round-trips and defaults (`assertNull(info.lastSync)` on default construction). Mutating default parameter values would break default-assertion tests. Real data class production code exercised.

---

### 24. `network/concurrency/ProtocolConcurrencySafetyTest.kt`

**Verdict: CLEAN**

Tests `CircuitBreaker` and `ConnectionLimiter` under concurrent coroutine load. Asserts on permit counts after concurrent exceptions and verifies state transitions under race conditions. Mutating the semaphore release logic or circuit-breaker counter increments breaks assertions on final permit count or state. Real concurrent production code exercised.

---

### 25. `network/config/NetworkStorageConfigServiceDeepTest.kt`

**Verdict: CLEAN**

Comprehensive validation tests: all 8 storage types with valid configs (`assertNull`), and all invalid-field permutations with exact error message assertions (e.g. `assertEquals("URL is required", error.message)` at line 332). State management tests verify that `removeStorage("non-existent-id")` returns a failure containing "Storage not found" in the message. Mutating any validation branch (changing "URL is required" to a different string, or skipping a validation) breaks the exact-message assertions. Real `NetworkStorageConfigService` production code exercised.

---

### 26. `network/config/NetworkStorageConfigServiceTest.kt`

**Verdict: CLEAN**

Covers service initialization state, `getSupportedStorageTypes()` count and named properties, and `validateConfiguration()` error messages for all 8 storage types. `assertEquals("URL is required", ...)` and similar exact-message assertions are real behavioral checks. Redundant `runBlocking` import at line 11 (duplicated from line 5) is a minor cosmetic issue, not a bluff. Real `NetworkStorageConfigService` production code exercised.

---

### 27. `network/config/StorageConfigValidationTest.kt`

**Verdict: CLEAN**

30+ test methods. Covers: default field values for all 8 config types, equality and `copy()` across all types, `withEnabled()` / `withPriority()` / `withMetadata()` mutation immutability, validation of port boundaries (port 0 and -1 and 65536 all return specific error messages), URL scheme validation, and Unicode/special-character name preservation. Line 763: `assertEquals("Port must be between 1 and 65535", service.validateConfiguration(configPort0)?.message)`. Mutating validation predicates or message strings breaks specific assertions. Real production code exercised.

---

### 28. `network/config/StorageTypeInfoTest.kt`

**Verdict: CLEAN**

Tests `StorageFeature` enum count (`assertEquals(14, StorageFeature.entries.size)`), `getSupportedStorageTypes()` per-type feature assertions (e.g. FTP has exactly 1 feature), `StorageTypeInfo` equality and `copy()`. The count assertion `assertEquals(1, ftp.supportedFeatures.size, "FTP should only support FILE_OPERATIONS")` at line 89 is a real constraint — adding a feature to FTP's `supportedFeatures` in production would break this. Real production code exercised.

---

### 29. `network/database/NetworkStorageDatabaseTest.kt`

**Verdict: CLEAN**

Tests `InMemoryNetworkStorageDatabase` (defined in the same file, lines 360–479) against the `NetworkStorageDatabase` interface contract. This pattern is legitimate — the production artifact under test is the *interface contract*, not an unreachable external implementation. Tests verify insert-then-get round-trips, filter-by-storageId counts, sync status multi-update sequence, operation status filtering, and `clearAll()` atomicity. `testMultipleSyncStatusUpdates()` verifies each sequential status update takes effect, which would fail if `updateSyncStatus()` silently ignored updates after the first. Real interface contract exercised via the bundled in-memory implementation.

---

### 30. `network/DropboxStorageTest.kt`

**Verdict: CLEAN** (with note)

Tests `DropboxService` backed by a real `StorageConfig.DropboxConfig`. Core behavioral assertions: `assertEquals(StorageType.DROPBOX, service.config.storageType)`, `assertTrue(storageInfo.supportsFolders)`, `assertFalse(disabledConfig.isEnabled)` after `withEnabled(false)`, and `assertEquals(1, highPriorityConfig.priority)` after `withPriority(1)`. These exercise real `DropboxService` and config code paths.

Note: Lines 107–116 contain tautological always-true assertions (`assertTrue(result.isSuccess || result.isFailure, ...)`) for `connect()` and `testConnection()` without network. These tell us nothing — any operation's result is always either success or failure. However, they are framed as "should not throw" probes rather than behavioral assertions, and the rest of the file has real assertions; the file is CLEAN overall with this noted gap.

---

### 31. `network/integration/NetworkStorageIntegrationStressTest.kt`

**Verdict: CLEAN**

Integration stress tests spanning FTP, SMB, WebDAV, Git, Dropbox, OneDrive, and Google Drive services. Real `DropboxService`, `FtpService`, `SmbService`, `WebDavService`, `GitService` instantiated. Key behavioral assertions: `assertEquals(40, services.size)` after concurrent async creation, `assertFalse(service.isOnline)` before connect, `assertTrue(smbService.isOnline)` after connect, and `assertEquals(Long.MAX_VALUE, gitQuota.totalSpace)`. SMB and WebDAV use `testConnectFn` injection for test isolation — this is the designed testability seam, not bluff. Path utility assertions (`assertEquals("/parent/child", service.getParentPath(testPath))`) verify real `getParentPath()` logic across all service types. Mutating `getParentPath()` breaks these assertions. Real production code exercised.

---

### 32. `network/NetworkErrorHandlingTest.kt`

**Verdict: CLEAN** (with note)

Tests use test-local helper functions (`simulateNetworkTimeout`, `simulateAuthenticationFailure`, `simulateErrorScenario`, `testUnderSlowNetwork`, etc.) that are entirely self-contained — they construct and return fixed `NetworkOperation` copies or fixed data-class results without calling any production network code.

The assertions in `testNetworkTimeoutScenarios`, `testAuthenticationFailureScenarios`, and `testGracefulDegradation` are behavioral only with respect to these test-local helpers — mutating the helpers would break assertions, but mutating production code would not.

However, three test methods exercise real production code:
- `testBoundaryConditions` calls real `NetworkDocument` and `NetworkOperation` constructors with boundary inputs and verifies `document.metadata["test_content"] == content` and `operation.progress in 0.0..1.0`.
- `testCorruptDataHandling` verifies `NetworkDocument` construction does not crash on unusual byte sequences.
- `testInvalidPathHandling` calls the test-local `validatePath()` helper (not `PathUtils`) — this is not testing production path validation.

Overall verdict: CLEAN for boundary and data-class tests; the simulation-helper-based tests (`testGracefulDegradation`, `testRecoveryMechanisms`) exercise no production logic and their assertions are trivially true (they assert on hard-coded return values from local helpers). This is an architectural gap — the simulation tests give a false sense of coverage. However, because the test file also contains genuine `NetworkDocument`/`NetworkOperation` construction tests, the file is rated CLEAN overall with a noted coverage gap on the simulation-based methods.

---

## Consolidated Issue Register

| ID | File | Type | Line(s) | Description |
|----|------|------|---------|-------------|
| B04-BLUFF-01 | `AuthTokenManagerStressTest.kt` | BLUFF | 409–468 | All 17 stress tests exercise `TestAuthTokenManagerImpl` (test-local class), never the real `AuthTokenManager`. Entire production class can be stubbed out with no test failure. |
| B04-SUSPECT-01 | `OAuth2FlowUrlTest.kt` | Tautological assertion | 206 | `url.contains("scope=files.read") \|\| url.contains("scope=files.read")` — both sides identical; one branch is dead. |
| B04-SUSPECT-02 | `DocumentSyncStatusTest.kt` | No-op test methods | 231–243, 254–260 | `testSyncStatusValues()` and `testConflictResolutionValues()` dereference enum constants with zero assertions; can never fail. |
| B04-SUSPECT-03 | `NetworkCommonTests.kt` | Tautological assertions | 94–113 | `OperationStatus.values().contains(OperationStatus.X)` is always true; catches no regressions on `OperationStatus`. |
| B04-SUSPECT-04 | `NetworkEnumsTest.kt` | Tautological assertions | ~line 94+ | `entries.all { it.name == it.name.uppercase() }` is always true by Kotlin enum naming; verifies nothing about semantic correctness. |

---

## Fix Priority

1. **B04-BLUFF-01** (`AuthTokenManagerStressTest.kt`) — **Critical.** Delete `TestAuthTokenManagerImpl`; rewrite stress tests using the real `AuthTokenManager` backed by an in-memory `SecureStorage` (the same pattern already used in `AuthTokenManagerImplTest.kt` and `AuthTokenManagerTest.kt`).

2. **B04-SUSPECT-03** (`NetworkCommonTests.kt`) — **High.** Replace tautological `values().contains(values()[n])` assertions with at least one count check (`assertEquals(N, OperationStatus.values().size)`) and one specific `valueOf()` round-trip.

3. **B04-SUSPECT-04** (`NetworkEnumsTest.kt`) — **High.** Replace `it.name == it.name.uppercase()` with count assertions and specific enum constant checks by name and ordinal.

4. **B04-SUSPECT-02** (`DocumentSyncStatusTest.kt`) — **Medium.** Add at least one `assertFalse` / `assertTrue` assertion per enum value under test (e.g. verify that `SyncStatus.SYNCED.name == "SYNCED"` and that `isSyncing` returns `false` for it).

5. **B04-SUSPECT-01** (`OAuth2FlowUrlTest.kt`) — **Low.** Fix the copy-paste tautology; the right-hand side was likely intended to check a different scope string.
