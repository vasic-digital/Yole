# Comprehensive Project Completion — Full Audit Report & Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring every module, application, library, test, and documentation artifact to 100% completion — zero unfinished features, zero dead code, zero concurrency hazards, maximum test coverage, full security scanning, complete documentation, updated video courses, and a fully current website.

**Architecture:** Fix-first approach — resolve all concurrency/safety issues and dead code before expanding tests, then layer on security scanning, documentation, and content updates. Each phase produces a shippable, non-breaking increment verified by the full test suite.

**Tech Stack:** Kotlin 2.0.20 (KMP), Compose Multiplatform 1.7.3, Ktor 3.0.2, Kotest 5.9.1, MockK 1.13.13, Docker/Podman, SonarQube Community, Snyk, Detekt 1.23.7, Gitleaks, OWASP Dependency Check 11.1.1, Go 1.24+ (Challenges/Containers), Next.js (website)

---

# PART 1: COMPREHENSIVE AUDIT REPORT

## 1.1 Current Project State (as of 2026-03-17)

| Metric | Value |
|--------|-------|
| Desktop tests passing | 6,695 (0 failures) |
| Total tests (all platforms) | ~9,400+ |
| Test files | 215 (195 commonTest, 16 desktopTest, 4 wasmJsTest) |
| Source files (commonMain) | 67 |
| Platform-specific source files | 37 (9 android, 9 desktop, 10 ios, 9 wasm) |
| Text format parsers | 17 |
| Network protocol services | 8 |
| Extracted KMP modules | 10 |
| Video course episodes | 21 |
| Website pages | 11 |
| Challenge banks | 21 |
| Documentation files | 758 total, 56 in docs/ |
| Architecture diagrams | 16 (8 Yole + 8 submodules, Mermaid) |
| Disabled/skipped tests | 0 |
| CI/CD workflows | 5 (ci, challenges, security, sonar, release) |

## 1.2 Unfinished Features & Dead Code

### 1.2.1 Platform Stubs (Intentional but Incomplete)

**iOS Platform — 3 protocol stubs:**
| File | Status | Reason |
|------|--------|--------|
| `iosMain/.../ftp/FtpProtocolClient.kt` | Stub returning `UnsupportedOperationException` | Raw TCP sockets unavailable; TODO: NWConnection (Network.framework) |
| `iosMain/.../sftp/SshClient.kt` | Stub returning `UnsupportedOperationException` | sshj is JVM-only; TODO: libssh2 cinterop |
| `iosMain/.../smb/SmbProtocolClient.kt` | Stub returning `UnsupportedOperationException` | smbj is JVM-only; TODO: libsmb2 cinterop |

**Wasm Platform — 3 protocol stubs + 2 enhancement TODOs:**
| File | Status | Reason |
|------|--------|--------|
| `wasmJsMain/.../ftp/FtpProtocolClient.kt` | Stub returning `UnsupportedOperationException` | Browser security prevents raw TCP; TODO: FTP-over-WebSocket proxy |
| `wasmJsMain/.../sftp/SshClient.kt` | Stub returning `UnsupportedOperationException` | No SSH in browser; TODO: SSH-over-WebSocket proxy |
| `wasmJsMain/.../smb/SmbProtocolClient.kt` | Stub returning `UnsupportedOperationException` | No SMB in browser; TODO: SMB-over-WebSocket proxy |
| `wasmJsMain/.../SecureStorageFactory.wasmJs.kt` | Uses localStorage (unencrypted) | TODO: Web Crypto API (SubtleCrypto) for AES-GCM, IndexedDB |
| `wasmJsMain/.../HttpClientFactory.wasmJs.kt` | Basic HTTP client | TODO: request timeout, content negotiation, retry logic |

### 1.2.2 Outdated Documentation
| File | Issue |
|------|-------|
| `FORMAT_DOCUMENTATION.md` (root) | References legacy platform-specific Java format structure; actual formats in KMP shared module |
| `TESTING_GUIDELINES.md` | Version 1.0 dated 2025-11-19; stale coverage numbers (56.01% branch), missing new test types |
| KMP module CHANGELOG/CONTRIBUTING files | 10 modules have minimal placeholder content |
| `docs/SESSION_*`, `docs/PHASE_*` files (24 files) | Historical session notes, not actively maintained |

### 1.2.3 Legacy Code
| Item | Status |
|------|--------|
| `commons/` module | Legacy `net.gsantner.opoc.*` namespace; `GsContextUtils` marked `@Deprecated` |
| `core/` module | Legacy Android module being phased out |
| `app/` module | Legacy Android app module |
| `doc/` directory | 5.4MB of Markor-era historical docs (2018-2023) |

## 1.3 Concurrency Safety Issues

### 1.3.1 CRITICAL Severity

**C1: Unprotected `_isConnected` reads in ALL 8 protocol services**
- Files: `DropboxService.kt`, `GoogleDriveService.kt`, `OneDriveService.kt`, `FtpService.kt`, `SftpService.kt`, `SmbService.kt`, `GitService.kt`, `WebDavService.kt`
- Problem: `_isConnected` is checked WITHOUT holding `stateMutex` in `getRecentChanges()`, `listFiles()`, `searchFiles()` — creating check-then-act race conditions
- Impact: "Client already closed" exceptions, resource leaks, use-after-close errors
- Fix: Read `_isConnected` under `stateMutex.withLock` in all public methods, or use `AtomicBoolean`

**C2: Unsafe `_httpClientAccessed` flag (Dropbox, GoogleDrive, OneDrive)**
- Files: Lines 84-88 in each service
- Problem: Non-atomic `var _httpClientAccessed` written inside `lazy {}` block, read in `disconnect()` without synchronization
- Impact: Race condition on lazy initialization, stale reads
- Fix: Replace with `AtomicBoolean` or remove flag and check `lazy.isInitialized()`

### 1.3.2 HIGH Severity

**H1: Double-checked locking anti-pattern in AuthTokenManager**
- File: `AuthTokenManager.kt:26-40`
- Problem: `_secureStorage` checked without lock, then re-checked inside `storageInitMutex.withLock`. Memory visibility not guaranteed.
- Fix: Always access `_secureStorage` under `storageInitMutex.withLock`, or use Kotlin's thread-safe `lazy`

**H2: Unprotected pauseFlags map mutations (Dropbox, GoogleDrive, OneDrive)**
- Files: Lines 109-110 in each service
- Problem: `pauseFlags` map entries created potentially without the `pauseFlagsMutex` lock
- Fix: Ensure all map insertions/reads are within `pauseFlagsMutex.withLock`

**H3: Unsafe StateFlow updates in NetworkStorageConfigService**
- File: `NetworkStorageConfigService.kt:27-31, 136-141`
- Problem: StateFlow `.value` read and write are not atomic within `mutex.withLock`
- Fix: Use `update { }` method on MutableStateFlow, or perform read-modify-write under lock

### 1.3.3 MEDIUM Severity

**M1: Flow emit() after cancellation in 3 cloud services**
- Files: `DropboxService.kt:1341`, `GoogleDriveService.kt:1412`, `OneDriveService.kt:1454`
- Problem: `emit(emptyList())` in catch block may itself throw `CancellationException`
- Fix: Add `ensureActive()` before emit in catch blocks

**M2: initJob silently swallowed exceptions (Dropbox, GoogleDrive, OneDrive)**
- Files: Lines 136-145 in each service
- Problem: Init job exceptions caught and ignored; never awaited in `connect()`
- Fix: Log exceptions, await initJob in connect(), add timeout

### 1.3.4 LOW Severity

**L1: ParsedDocument HTML cache not thread-safe** — `TextParser.kt:75-80` — duplicate work only, no corruption
**L2: StyleSheets cache not synchronized** — `StyleSheets.kt:49-82` — duplicate work only
**L3: DocumentCache statistics not atomic** — `DocumentCache.kt:27-33` — inconsistent stats only

## 1.4 Test Coverage Gaps

### 1.4.1 Source Files with Sparse or Missing Dedicated Tests

| Source File | Current Tests | Gap |
|-------------|--------------|-----|
| `TextFormat.kt` | 7 | Needs detection pattern tests, extension matching, metadata access |
| `RateLimiting.kt` (facade) | 16 | Minimal but acceptable for typealias facade |
| `StorageModelsTest.kt` | 8 | Minimal for data classes |
| `CircuitBreaker.kt` | Indirect (86 via resilience) | No isolated unit tests |
| `ConnectionLimiter.kt` | Indirect (86 via resilience) | No isolated unit tests |
| `RestructuredTextParser` | 59 | Lowest format count; missing edge-case variants |

### 1.4.2 Platform-Specific Test Gaps

| Platform | Test Files | Status |
|----------|-----------|--------|
| commonTest | 195 | Comprehensive |
| desktopTest | 16 | Good (memory, performance, MockK) |
| androidUnitTest | 0 | No Android-specific unit tests |
| wasmJsTest | 4 (132 tests) | Minimal — only stubs and detection |
| iosTest | 0 | No iOS-specific tests (expected — no iOS build on this host) |

### 1.4.3 Test Types Present (All 16 Required Types)

| Test Type | Present | Count | Gap |
|-----------|---------|-------|-----|
| Unit | Yes | 1,437+ | None |
| Integration | Yes | 71 | Could expand cross-module |
| Stress | Yes | 263 | Could add per-protocol stress |
| Supremacy/Edge-case | Yes | 12 | Single file only |
| Mock HTTP | Yes | 312 | FTP/SFTP/SMB missing (not HTTP-based) |
| Property-based | Yes | 19 | Could expand to more formats |
| Contract | Yes | 89 | Complete for all 8 protocols |
| Security | Yes | 57 | Could add OWASP-specific tests |
| Performance | Yes | 144 | Could add regression baselines |
| Resilience | Yes | 86 | Complete |
| Fuzz | Yes | 23 | Could expand per-format |
| Snapshot | Yes | 46 | Complete |
| Load | Yes | 22 | Could add protocol load tests |
| E2E | Yes | 102 | Complete |
| Accessibility | Yes | 230 | Complete |
| Non-blocking | Yes | 25 | Could expand to network layer |

## 1.5 Container & Security Infrastructure Gaps

### 1.5.1 Docker/Container Issues
| Issue | Severity | Detail |
|-------|----------|--------|
| No health checks | High | SonarQube, Snyk containers have no healthcheck directives |
| No resource limits | High | No `mem_limit`, `cpus` — causes OOM kills (exit 137) |
| Dockerfile runs as root | Medium | No non-root user created; `privileged: true` breaks rootless Podman |
| Hardcoded test keystore credentials | Low | `yole123` in `docker/scripts/build.sh` — dev only |
| No restart policies | Medium | Containers don't recover from crashes |

### 1.5.2 Security Scanning Gaps
| Tool | Status | Gap |
|------|--------|-----|
| SonarQube | Configured | No local auth setup; needs token for CI |
| Snyk | Minimal `.snyk` | No patch rules, no exemptions configured |
| Detekt | Active (`maxIssues: 0`) | Working correctly |
| Gitleaks | Comprehensive (8 custom rules) | Working correctly |
| OWASP Dependency Check | CVSS threshold 9.0 | Too high — allows 8.x high-severity vulns |
| CodeQL | In security workflow | No `.github/codeql/codeql-config.yml` file |
| Coverage gate | Not enforced | No minimum coverage threshold in CI |
| SBOM | Missing | No Software Bill of Materials generation |
| Pre-commit hooks | Missing | No automated checks before git push |

## 1.6 Documentation Gaps Summary

| Category | Status | Action Needed |
|----------|--------|---------------|
| Root .md files (15) | 14 Complete, 1 Outdated | Rewrite FORMAT_DOCUMENTATION.md |
| docs/ directory (56 files) | 45 Complete, 11 Historical | Archive session notes, update stale refs |
| Architecture diagrams (16) | Complete | Add concurrency/safety diagram |
| Website (11 pages) | Complete | Update stats, add new content sections |
| Video course (21 scripts) | Complete | Add new episodes for new features |
| KMP module docs (10) | Placeholder content | Expand CHANGELOG/CONTRIBUTING |
| TESTING_GUIDELINES.md | Outdated (v1.0) | Update to v2.0 with current metrics |
| User manuals | Partial | Need step-by-step platform guides |
| SQL/Schema definitions | N/A (no SQL) | DATABASE_SCHEMA.md covers entity model |

---

# PART 2: PHASED IMPLEMENTATION PLAN

## Constraints (GitSpec, CLAUDE.md, AGENTS.md)

1. **GitSpec Constitution**: Conventional Commits format, PR review, no force pushes to master, all tests pass before merge
2. **CLAUDE.md**: Release builds/CI tests in containers; no tests removed/disabled/skipped; SPDX headers; fixes covered by all test types
3. **AGENTS.md**: 70%+ coverage enforced; division by package boundary; thread safety across packages; no circular dependencies
4. **Non-Interactive**: No sudo/root prompts; all container ops non-interactive
5. **Non-Breaking**: All changes additive or fix-only; existing functionality preserved

---

## Phase 1: Concurrency Safety & Race Condition Fixes

**Goal:** Eliminate all identified race conditions, deadlocks, and memory leak risks.

**Estimated scope:** ~15 files modified, ~8 new test files

### Task 1.1: Fix _isConnected Race Condition in All 8 Protocol Services

**Files:**
- Modify: `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/dropbox/DropboxService.kt`
- Modify: `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/googledrive/GoogleDriveService.kt`
- Modify: `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/onedrive/OneDriveService.kt`
- Modify: `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/sftp/SftpService.kt`
- Modify: `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/smb/SmbService.kt`
- Modify: `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/ftp/FtpService.kt`
- Modify: `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/git/GitService.kt`
- Modify: `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/webdav/WebDavService.kt`
- Test: `shared/src/commonTest/kotlin/digital/vasic/yole/network/concurrency/ProtocolConcurrencySafetyTest.kt`

- [ ] **Step 1: Create comprehensive concurrency test for protocol _isConnected race**

Write test that launches concurrent `connect()`, `disconnect()`, and `listFiles()` calls to reproduce the race.

```kotlin
@Test
fun `concurrent connect disconnect should not cause use-after-close`() = runBlocking<Unit> {
    val service = DropboxService(config)
    val errors = mutableListOf<Throwable>()
    val jobs = (1..100).map {
        launch(Dispatchers.Default) {
            try {
                if (it % 3 == 0) service.disconnect()
                else if (it % 3 == 1) service.connect()
                else service.listFiles("/").first()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { synchronized(errors) { errors.add(e) } }
        }
    }
    jobs.forEach { it.join() }
    // No IllegalStateException("Client already closed") should appear
    assertTrue(errors.none { it.message?.contains("closed") == true })
}
```

- [ ] **Step 2: Run test to verify it fails** (reproduces the race)

Run: `./gradlew :shared:desktopTest --tests "digital.vasic.yole.network.concurrency.ProtocolConcurrencySafetyTest"`
Expected: FAIL or flaky due to race condition

- [ ] **Step 3: Replace `var _isConnected` with `AtomicBoolean` in all 8 services**

In each service file, change:
```kotlin
// BEFORE
private var _isConnected = false

// AFTER
private val _isConnected = kotlinx.atomicfu.atomic(false)
```

Update all reads from `_isConnected` to `_isConnected.value` and all writes from `_isConnected = true` to `_isConnected.value = true`. Remove `stateMutex.withLock` around simple boolean sets.

- [ ] **Step 4: Run full test suite to verify no regressions**

Run: `./gradlew :shared:desktopTest`
Expected: All 6,695+ tests PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/
git add shared/src/commonTest/kotlin/digital/vasic/yole/network/concurrency/
git commit -m "fix: replace _isConnected var with AtomicBoolean in all 8 protocol services

Eliminates check-then-act race condition where disconnect() could close
httpClient between _isConnected check and actual HTTP call in listFiles(),
getRecentChanges(), searchFiles(), etc.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

### Task 1.2: Fix _httpClientAccessed Flag Race (Dropbox, GoogleDrive, OneDrive)

**Files:**
- Modify: `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/dropbox/DropboxService.kt:84-88`
- Modify: `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/googledrive/GoogleDriveService.kt:83-87`
- Modify: `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/onedrive/OneDriveService.kt:84-88`

- [ ] **Step 1: Replace `_httpClientAccessed` var with lazy `isInitialized()` check**

In each of the 3 services:
```kotlin
// BEFORE
private var _httpClientAccessed = false
private val httpClient by lazy {
    _httpClientAccessed = true
    _injectedHttpClient ?: createHttpClient()
}

// AFTER — use a backing lazy delegate we can check isInitialized() on
private val _httpClientLazy = lazy { _injectedHttpClient ?: createHttpClient() }
private val httpClient by _httpClientLazy
```

In `disconnect()`, replace `if (_httpClientAccessed)` with `if (_httpClientLazy.isInitialized())`.

- [ ] **Step 2: Run full test suite**

Run: `./gradlew :shared:desktopTest`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git commit -m "fix: eliminate _httpClientAccessed race with lazy isInitialized()

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

### Task 1.3: Fix AuthTokenManager Double-Checked Locking

**Files:**
- Modify: `shared/src/commonMain/kotlin/digital/vasic/yole/network/auth/AuthTokenManager.kt:26-40`
- Test: `shared/src/commonTest/kotlin/digital/vasic/yole/network/auth/AuthTokenManagerConcurrencyTest.kt`

- [ ] **Step 1: Write concurrent initialization test**

```kotlin
@Test
fun `concurrent getSecureStorage should initialize exactly once`() = runBlocking<Unit> {
    val manager = AuthTokenManagerImpl(config)
    val results = (1..50).map {
        async(Dispatchers.Default) { manager.getToken() }
    }
    results.awaitAll()
    // Verify single initialization via internal state
}
```

- [ ] **Step 2: Replace double-checked locking with thread-safe lazy**

```kotlin
// BEFORE
private var _secureStorage: SecureStorage? = null
private val storageInitMutex = Mutex()
private suspend fun getSecureStorage(): SecureStorage {
    _secureStorage?.let { return it }
    return storageInitMutex.withLock {
        _secureStorage ?: SecureStorageFactory.create().getOrThrow().also {
            _secureStorage = it
        }
    }
}

// AFTER
private val _secureStorageLazy = lazy {
    kotlinx.coroutines.runBlocking { SecureStorageFactory.create().getOrThrow() }
}
private fun getSecureStorage(): SecureStorage = _secureStorageLazy.value
```

Note: If `SecureStorageFactory.create()` must be called from a suspend context, keep the mutex but remove the unprotected first check:

```kotlin
private suspend fun getSecureStorage(): SecureStorage {
    return storageInitMutex.withLock {
        _secureStorage ?: SecureStorageFactory.create().getOrThrow().also {
            _secureStorage = it
        }
    }
}
```

- [ ] **Step 3: Run tests, commit**

### Task 1.4: Fix pauseFlags Map Synchronization

**Files:**
- Modify: `DropboxService.kt`, `GoogleDriveService.kt`, `OneDriveService.kt`

- [ ] **Step 1: Audit all `pauseFlags` access points — ensure ALL are within `pauseFlagsMutex.withLock`**

Search for `pauseFlags[` and `pauseFlags.` in each file. Wrap any unprotected access.

- [ ] **Step 2: Run tests, commit**

### Task 1.5: Fix NetworkStorageConfigService StateFlow Updates

**Files:**
- Modify: `shared/src/commonMain/kotlin/digital/vasic/yole/network/config/NetworkStorageConfigService.kt:136-141`

- [ ] **Step 1: Replace read-then-write StateFlow pattern with `update {}`**

```kotlin
// BEFORE
val currentStorages = _configuredStorages.value.toMutableList()
_configuredStorages.value = currentStorages + networkStorage

// AFTER
_configuredStorages.update { it + networkStorage }
```

- [ ] **Step 2: Run tests, commit**

### Task 1.6: Fix Flow Cancellation Safety in Cloud Services

**Files:**
- Modify: `DropboxService.kt:1341-1344`, `GoogleDriveService.kt:1412-1415`, `OneDriveService.kt:1454-1457`

- [ ] **Step 1: Add `currentCoroutineContext().ensureActive()` before `emit()` in catch blocks**

```kotlin
} catch (e: Exception) {
    if (e is kotlin.coroutines.cancellation.CancellationException) throw e
    currentCoroutineContext().ensureActive()
    emit(emptyList())
}
```

- [ ] **Step 2: Run tests, commit**

### Task 1.7: Add Logging for initJob Exceptions

**Files:**
- Modify: `DropboxService.kt:136-145`, `GoogleDriveService.kt`, `OneDriveService.kt`

- [ ] **Step 1: Replace silent catch with logged warning**

```kotlin
init {
    initJob = serviceScope.launch {
        try {
            initializeConnection()
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            // Log initialization failure for debugging
            println("WARNING: ${this::class.simpleName} init failed: ${e.message}")
        }
    }
}
```

- [ ] **Step 2: In `connect()`, await initJob with timeout**

```kotlin
override suspend fun connect(): Result<Unit> {
    initJob?.let {
        withTimeoutOrNull(5000) { it.join() }
    }
    // ... existing connect logic
}
```

- [ ] **Step 3: Run tests, commit**

### Task 1.8: Synchronize TextParser HTML Cache and StyleSheets Cache

**Files:**
- Modify: `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextParser.kt:75-80`
- Modify: `shared/src/commonMain/kotlin/digital/vasic/yole/format/StyleSheets.kt:49-82`
- Modify: `shared/src/commonMain/kotlin/digital/vasic/yole/format/DocumentCache.kt:27-33`

- [ ] **Step 1: Use `@Volatile` on HTML cache fields and double-check pattern**

```kotlin
@Volatile private var _cachedHtmlLight: String? = null
@Volatile private var _cachedHtmlDark: String? = null
```

- [ ] **Step 2: Use `ConcurrentHashMap` equivalent for StyleSheets cache**

For KMP common, wrap with synchronized access:
```kotlin
private val styleSheetCache = mutableMapOf<String, String>()
private val cacheLock = Any()

fun getStyleSheet(formatId: String, lightMode: Boolean): String {
    val cacheKey = "$formatId:$lightMode"
    synchronized(cacheLock) {
        styleSheetCache[cacheKey]?.let { return it }
    }
    val result = computeStyleSheet(formatId, lightMode)
    synchronized(cacheLock) {
        styleSheetCache.putIfAbsent(cacheKey, result) ?: result
    }
    return result
}
```

Note: In KMP `commonMain`, use `digital.vasic.yole.util.platformSynchronized()` which delegates to the platform's synchronization primitive.

- [ ] **Step 3: Make DocumentCache statistics reads atomic**

```kotlin
fun getStats(): CacheStats {
    return mutex.withLock { CacheStats(_hits, _misses) }
}
```

Or use `AtomicLong` for `_hits` and `_misses`.

- [ ] **Step 4: Run full test suite, commit**

```bash
git commit -m "fix: synchronize HTML cache, stylesheet cache, and document cache statistics

Prevents duplicate computation in concurrent scenarios and ensures
consistent statistics reads.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

### Task 1.9: Create Comprehensive Concurrency Safety Tests

**Files:**
- Create: `shared/src/commonTest/kotlin/digital/vasic/yole/network/concurrency/ProtocolConcurrencySafetyTest.kt`
- Create: `shared/src/commonTest/kotlin/digital/vasic/yole/network/concurrency/StateFlowConcurrencyTest.kt`
- Create: `shared/src/commonTest/kotlin/digital/vasic/yole/format/concurrency/CacheConcurrencyTest.kt`

- [ ] **Step 1: Write concurrent connect/disconnect tests for all 8 protocols**
- [ ] **Step 2: Write concurrent StateFlow update tests for NetworkStorageConfigService**
- [ ] **Step 3: Write concurrent cache access tests for TextParser, StyleSheets, DocumentCache**
- [ ] **Step 4: Run all new tests, verify PASS**
- [ ] **Step 5: Commit all concurrency safety tests**

---

## Phase 2: Dead Code Elimination & Platform Enhancement

**Goal:** Remove all dead code, complete platform stubs where possible, update deprecated APIs.

### Task 2.1: Enhance Wasm SecureStorage with Web Crypto API

**Files:**
- Modify: `shared/src/wasmJsMain/kotlin/digital/vasic/yole/network/platform/SecureStorageFactory.wasmJs.kt`
- Modify: `shared/src/wasmJsMain/kotlin/digital/vasic/yole/network/platform/WebSecureStorage.kt`
- Test: `shared/src/wasmJsTest/kotlin/digital/vasic/yole/network/platform/WebSecureStorageEncryptionTest.kt`

- [ ] **Step 1: Implement AES-GCM encryption via Web Crypto API (SubtleCrypto)**
- [ ] **Step 2: Add IndexedDB backend option for larger storage**
- [ ] **Step 3: Write tests for encrypted storage operations**
- [ ] **Step 4: Run wasm tests, commit**

### Task 2.2: Enhance Wasm HttpClientFactory with Retry and Timeout

**Files:**
- Modify: `shared/src/wasmJsMain/kotlin/digital/vasic/yole/network/protocol/HttpClientFactory.wasmJs.kt`

- [ ] **Step 1: Add request timeout configuration**
- [ ] **Step 2: Add content negotiation plugin (JSON serialization)**
- [ ] **Step 3: Add retry logic for transient network failures**
- [ ] **Step 4: Run tests, commit**

### Task 2.3: Improve iOS Platform Stubs with Better Error Reporting

**Files:**
- Modify: `shared/src/iosMain/kotlin/digital/vasic/yole/network/protocols/ftp/FtpProtocolClient.kt`
- Modify: `shared/src/iosMain/kotlin/digital/vasic/yole/network/protocols/sftp/SshClient.kt`
- Modify: `shared/src/iosMain/kotlin/digital/vasic/yole/network/protocols/smb/SmbProtocolClient.kt`

- [ ] **Step 1: Add detailed error messages explaining platform limitation**
- [ ] **Step 2: Add `@OptIn(ExperimentalApi::class)` annotations where needed for future NWConnection work**
- [ ] **Step 3: Ensure all methods return `Result.failure` with descriptive `PlatformNotSupportedException`**
- [ ] **Step 4: Commit**

### Task 2.4: Update Deprecated GsContextUtils Reference

**Files:**
- Audit: `commons/src/main/kotlin/digital/vasic/opoc/util/GsContextUtils.kt`

- [ ] **Step 1: Verify no new code references GsContextUtils — only legacy module uses it**
- [ ] **Step 2: Add `@Suppress("DEPRECATION")` where legacy callers exist**
- [ ] **Step 3: Document removal plan in LEGACY_MIGRATION.md**
- [ ] **Step 4: Commit**

---

## Phase 3: Test Coverage Expansion to Maximum

**Goal:** Increase test coverage to theoretical maximum across all test types and all source files.

### Task 3.1: Add Isolated CircuitBreaker Unit Tests

**Files:**
- Create: `shared/src/commonTest/kotlin/digital/vasic/yole/network/common/CircuitBreakerUnitTest.kt`

- [ ] **Step 1: Write tests for all state transitions (CLOSED → OPEN → HALF_OPEN → CLOSED)**
- [ ] **Step 2: Write tests for failure threshold triggering**
- [ ] **Step 3: Write tests for timeout-based recovery**
- [ ] **Step 4: Write tests for concurrent access**
- [ ] **Step 5: Run, verify PASS, commit** (target: 40+ tests)

### Task 3.2: Add Isolated ConnectionLimiter Unit Tests

**Files:**
- Create: `shared/src/commonTest/kotlin/digital/vasic/yole/network/common/ConnectionLimiterUnitTest.kt`

- [ ] **Step 1: Write tests for acquire/release semantics**
- [ ] **Step 2: Write tests for max connection enforcement**
- [ ] **Step 3: Write tests for timeout behavior**
- [ ] **Step 4: Write tests for concurrent acquire/release**
- [ ] **Step 5: Run, verify PASS, commit** (target: 30+ tests)

### Task 3.3: Expand TextFormat Dedicated Tests

**Files:**
- Create: `shared/src/commonTest/kotlin/digital/vasic/yole/format/TextFormatComprehensiveTest.kt`

- [ ] **Step 1: Write tests for all 17 format ID constants**
- [ ] **Step 2: Write tests for extension detection (all known extensions per format)**
- [ ] **Step 3: Write tests for content detection patterns (regex matching)**
- [ ] **Step 4: Write tests for metadata (name, description, MIME types)**
- [ ] **Step 5: Write edge-case tests (unknown extensions, ambiguous content)**
- [ ] **Step 6: Run, verify PASS, commit** (target: 60+ tests)

### Task 3.4: Expand RestructuredText Tests

**Files:**
- Create: `shared/src/commonTest/kotlin/digital/vasic/yole/format/rst/RstParserEdgeCaseTest.kt`
- Create: `shared/src/commonTest/kotlin/digital/vasic/yole/format/rst/RstParserVariantsTest.kt`

- [ ] **Step 1: Write edge-case tests (nested directives, malformed RST, deeply nested lists)**
- [ ] **Step 2: Write variant tests (different heading styles, doctest blocks, substitutions)**
- [ ] **Step 3: Run, verify PASS, commit** (target: 40+ new tests)

### Task 3.5: Add Android Platform Unit Tests

**Files:**
- Create: `shared/src/androidUnitTest/kotlin/digital/vasic/yole/network/platform/AndroidSecureStorageTest.kt`
- Create: `shared/src/androidUnitTest/kotlin/digital/vasic/yole/format/todotxt/AndroidTodoTxtParserTest.kt`

Note: These require Android SDK / Robolectric and run inside containers.

- [ ] **Step 1: Write Android-specific SecureStorage tests (Keystore, EncryptedSharedPreferences)**
- [ ] **Step 2: Write Android-specific TodoTxtParser tests (File I/O, SAF)**
- [ ] **Step 3: Run in container, verify PASS, commit** (target: 25+ tests)

### Task 3.6: Expand Wasm Platform Tests

**Files:**
- Create: `shared/src/wasmJsTest/kotlin/digital/vasic/yole/format/WasmFormatParsingTests.kt`
- Create: `shared/src/wasmJsTest/kotlin/digital/vasic/yole/network/platform/WasmHttpClientFactoryTest.kt`

- [ ] **Step 1: Write Wasm-specific format parsing tests for all 17 formats**
- [ ] **Step 2: Write Wasm-specific HttpClientFactory tests**
- [ ] **Step 3: Run, verify PASS, commit** (target: 50+ tests)

### Task 3.7: Expand Property-Based Tests

**Files:**
- Create: `shared/src/commonTest/kotlin/digital/vasic/yole/format/property/PropertyBasedParserTests.kt`

- [ ] **Step 1: Add property-based tests for all 17 formats (parse → toHtml never crashes)**
- [ ] **Step 2: Add property-based tests for PathUtils (normalization idempotent)**
- [ ] **Step 3: Add property-based tests for DocumentCache (get after put always returns)**
- [ ] **Step 4: Run, verify PASS, commit** (target: 40+ tests)

### Task 3.8: Expand Fuzz Tests Per Format

**Files:**
- Modify: `shared/src/commonTest/kotlin/digital/vasic/yole/format/fuzz/FormatFuzzTests.kt`
- Create: `shared/src/commonTest/kotlin/digital/vasic/yole/format/fuzz/FormatFuzzExtendedTests.kt`

- [ ] **Step 1: Add fuzz tests with null bytes, control characters, extreme Unicode**
- [ ] **Step 2: Add fuzz tests with extremely nested structures**
- [ ] **Step 3: Add fuzz tests with >1MB inputs per format**
- [ ] **Step 4: Run, verify PASS, commit** (target: 40+ new tests)

### Task 3.9: Add Protocol-Specific Stress Tests

**Files:**
- Create: `shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/ProtocolOverloadStressTest.kt`
- Create: `shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/ProtocolConcurrentOperationsStressTest.kt`

- [ ] **Step 1: Write tests that submit 1000 concurrent operations per protocol**
- [ ] **Step 2: Write tests that rapidly connect/disconnect under load**
- [ ] **Step 3: Write tests that verify backpressure handling**
- [ ] **Step 4: Run, verify PASS, commit** (target: 30+ tests)

### Task 3.10: Add OWASP Security Tests

**Files:**
- Create: `shared/src/commonTest/kotlin/digital/vasic/yole/security/OwaspSecurityTest.kt`

- [ ] **Step 1: Write path traversal tests for all protocol services**
- [ ] **Step 2: Write injection tests (command injection, header injection)**
- [ ] **Step 3: Write authentication bypass tests**
- [ ] **Step 4: Write sensitive data exposure tests (tokens in logs, error messages)**
- [ ] **Step 5: Run, verify PASS, commit** (target: 40+ tests)

### Task 3.11: Add Network Layer Non-Blocking Tests

**Files:**
- Create: `shared/src/commonTest/kotlin/digital/vasic/yole/network/nonblocking/NetworkNonBlockingTest.kt`

- [ ] **Step 1: Write tests verifying no blocking calls in coroutine context**
- [ ] **Step 2: Write tests verifying Dispatchers.IO usage for I/O operations**
- [ ] **Step 3: Write tests verifying timeout behavior**
- [ ] **Step 4: Run, verify PASS, commit** (target: 25+ tests)

### Task 3.12: Add Monitoring and Metrics Collection Tests

**Files:**
- Create: `shared/src/commonTest/kotlin/digital/vasic/yole/monitoring/MetricsCollectionTest.kt`
- Create: `shared/src/commonTest/kotlin/digital/vasic/yole/monitoring/PerformanceRegressionTest.kt`

- [ ] **Step 1: Write tests that collect parse time per format and assert <100ms for typical documents**
- [ ] **Step 2: Write tests that collect cache hit rates and assert >80% for repeated access**
- [ ] **Step 3: Write tests that collect memory usage before/after parsing and assert no leaks**
- [ ] **Step 4: Write tests that collect operation latency per protocol and assert <500ms for mock responses**
- [ ] **Step 5: Run, verify PASS, commit** (target: 40+ tests)

### Task 3.13: Add Lazy Loading and Semaphore Effectiveness Tests

**Files:**
- Create: `shared/src/commonTest/kotlin/digital/vasic/yole/performance/LazyInitPerformanceTest.kt`
- Create: `shared/src/commonTest/kotlin/digital/vasic/yole/performance/SemaphoreBackpressureTest.kt`

- [ ] **Step 1: Write tests verifying FormatRegistry lazy init delays until first access**
- [ ] **Step 2: Write tests verifying StyleSheets cache prevents re-computation**
- [ ] **Step 3: Write tests verifying ConnectionLimiter enforces max concurrent connections**
- [ ] **Step 4: Write tests verifying semaphore fairness under contention**
- [ ] **Step 5: Run, verify PASS, commit** (target: 30+ tests)

---

## Phase 4: Security Scanning Infrastructure & Execution

**Goal:** Make all security scanning tools fully operational via containers (Docker/Podman), execute scans, analyze findings, resolve issues.

### Task 4.1: Harden Docker Infrastructure

**Files:**
- Modify: `docker-compose.yml`
- Modify: `docker/yole-build/Dockerfile`

- [ ] **Step 1: Add health checks to all services**

```yaml
sonarqube:
  image: sonarqube:community
  healthcheck:
    test: ["CMD-SHELL", "curl -f http://localhost:9000/api/system/status || exit 1"]
    interval: 30s
    timeout: 10s
    retries: 5
    start_period: 120s
```

- [ ] **Step 2: Add resource limits**

```yaml
services:
  build:
    mem_limit: 4g
    memswap_limit: 6g
    cpus: 2
  sonarqube:
    mem_limit: 2g
    memswap_limit: 3g
```

- [ ] **Step 3: Create non-root user in Dockerfile**

```dockerfile
RUN groupadd -r yole && useradd -r -g yole -m yole
USER yole
```

- [ ] **Step 4: Remove `privileged: true` for rootless Podman compatibility**
- [ ] **Step 5: Add restart policies**

```yaml
restart: unless-stopped
```

- [ ] **Step 6: Run `podman compose build build` to verify**
- [ ] **Step 7: Commit**

### Task 4.2: Configure SonarQube for Local Analysis

**Files:**
- Modify: `sonar-project.properties`
- Create: `docker/scripts/setup-sonarqube.sh`

- [ ] **Step 1: Create non-interactive SonarQube setup script**

```bash
#!/bin/bash
# Wait for SonarQube to be ready
until curl -s http://localhost:9000/api/system/status | grep -q '"status":"UP"'; do
    sleep 5
done
# Change default password (non-interactive)
curl -u admin:admin -X POST "http://localhost:9000/api/users/change_password?login=admin&previousPassword=admin&password=yole-sonar-2026"
# Create project token
curl -u admin:yole-sonar-2026 -X POST "http://localhost:9000/api/user_tokens/generate?name=yole-local"
```

- [ ] **Step 2: Run SonarQube container and execute setup**

```bash
podman compose --profile security up -d sonarqube
./docker/scripts/setup-sonarqube.sh
```

- [ ] **Step 3: Run SonarQube analysis**

```bash
./gradlew :shared:desktopTest koverXmlReport
./gradlew sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<generated-token>
```

- [ ] **Step 4: Commit setup script**

### Task 4.3: Lower OWASP CVSS Threshold

**Files:**
- Modify: `build.gradle.kts`

- [ ] **Step 1: Change `failBuildOnCVSS` from 9.0 to 7.0**

```kotlin
dependencyCheck {
    failBuildOnCVSS = 7.0f  // Fail on high + critical vulnerabilities
}
```

- [ ] **Step 2: Run `./gradlew dependencyCheckAnalyze` to identify any new failures**
- [ ] **Step 3: Add suppressions for known false positives if needed**
- [ ] **Step 4: Commit**

### Task 4.4: Configure CodeQL

**Files:**
- Create: `.github/codeql/codeql-config.yml`

- [ ] **Step 1: Create CodeQL configuration**

```yaml
name: "Yole CodeQL Configuration"
queries:
  - uses: security-and-quality
paths:
  - shared/src/commonMain
  - shared/src/androidMain
  - shared/src/desktopMain
  - androidApp/src
  - desktopApp/src
paths-ignore:
  - '**/test/**'
  - '**/build/**'
  - 'doc/**'
```

- [ ] **Step 2: Update security.yml workflow to reference config**
- [ ] **Step 3: Commit**

### Task 4.5: Execute Full Security Scan and Resolve Findings

- [ ] **Step 1: Run Detekt** — `./gradlew detekt`
- [ ] **Step 2: Run OWASP Dependency Check** — `./gradlew dependencyCheckAnalyze`
- [ ] **Step 3: Run Gitleaks** — `gitleaks detect --source . --config .gitleaks.toml` (install if needed via container)
- [ ] **Step 4: Run local security scan script** — `./scripts/run_security_scan.sh`
- [ ] **Step 5: Analyze all findings, categorize by severity**
- [ ] **Step 6: Fix all HIGH and CRITICAL findings**
- [ ] **Step 7: Document all MEDIUM findings with justification if not fixed**
- [ ] **Step 8: Generate security scan report**
- [ ] **Step 9: Commit all fixes**

### Task 4.6: Add Coverage Gate to CI

**Files:**
- Modify: `.github/workflows/ci.yml`

- [ ] **Step 1: Add minimum coverage enforcement**

```yaml
- name: Check coverage threshold
  run: |
    COVERAGE=$(cat shared/build/reports/kover/report.xml | grep -o 'covered="[0-9]*"' | head -1 | grep -o '[0-9]*')
    TOTAL=$((COVERAGE + $(cat shared/build/reports/kover/report.xml | grep -o 'missed="[0-9]*"' | head -1 | grep -o '[0-9]*')))
    PCT=$((COVERAGE * 100 / TOTAL))
    echo "Coverage: ${PCT}%"
    if [ $PCT -lt 70 ]; then echo "Coverage below 70%!" && exit 1; fi
```

- [ ] **Step 2: Commit**

### Task 4.7: Generate SBOM in Release Workflow

**Files:**
- Modify: `.github/workflows/release.yml`
- Modify: `build.gradle.kts`

- [ ] **Step 1: Add CycloneDX Gradle plugin for SBOM generation**

```kotlin
id("org.cyclonedx.bom") version "1.10.0"
```

- [ ] **Step 2: Add SBOM generation step to release workflow**

```yaml
- name: Generate SBOM
  run: ./gradlew cyclonedxBom
- name: Upload SBOM
  uses: actions/upload-artifact@v4
  with:
    name: sbom
    path: build/reports/bom.json
```

- [ ] **Step 3: Commit**

---

## Phase 5: Performance Optimization — Lazy Loading, Semaphores, Non-Blocking

**Goal:** Ensure every component uses lazy initialization, semaphore-based concurrency control, and non-blocking I/O.

### Task 5.1: Audit and Enforce Lazy Initialization Everywhere

**Files:**
- Audit all `init {}` blocks in protocol services, parsers, registries
- Modify as needed to defer initialization

- [ ] **Step 1: Audit all eager initializations in network services**
- [ ] **Step 2: Convert any eager HTTP client creation to lazy**
- [ ] **Step 3: Verify ParserInitializer uses lazy registration**
- [ ] **Step 4: Write test verifying no eager initialization on import**
- [ ] **Step 5: Run full suite, commit**

### Task 5.2: Add Semaphore Controls to Format Parsing

**Files:**
- Modify: `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt`

- [ ] **Step 1: Add semaphore to limit concurrent parse operations**

```kotlin
private val parseSemaphore = Semaphore(permits = Runtime.getRuntime().availableProcessors())

suspend fun parseWithCacheConcurrent(content: String, format: TextFormat): ParsedDocument {
    return parseSemaphore.withPermit {
        parseWithCache(content, format)
    }
}
```

- [ ] **Step 2: Write test verifying semaphore limits concurrent parsing**
- [ ] **Step 3: Run suite, commit**

### Task 5.3: Ensure All I/O Operations Are Non-Blocking

**Files:**
- Audit all protocol service methods for blocking calls

- [ ] **Step 1: Verify all file I/O wrapped in `withContext(Dispatchers.IO)`**
- [ ] **Step 2: Verify all network calls use Ktor's non-blocking engine**
- [ ] **Step 3: Write tests that verify no thread blocking in main dispatcher**
- [ ] **Step 4: Run suite, commit**

### Task 5.4: Add Performance Regression Baselines

**Files:**
- Create: `shared/src/commonTest/kotlin/digital/vasic/yole/performance/PerformanceBaselineRegressionTest.kt`

- [ ] **Step 1: Establish baseline times for all 17 format parsers**
- [ ] **Step 2: Establish baseline cache hit rates**
- [ ] **Step 3: Establish baseline memory footprints**
- [ ] **Step 4: Write regression tests that fail if >20% degradation**
- [ ] **Step 5: Run suite, commit**

---

## Phase 6: Challenge Framework Expansion

**Goal:** Add new challenge banks covering all new test types, concurrency fixes, security scanning, and performance.

### Task 6.1: Create Concurrency Safety Challenge Bank

**Files:**
- Create: `Challenges/banks/yole/concurrency-safety-challenges.json`

- [ ] **Step 1: Define challenges for AtomicBoolean usage**
- [ ] **Step 2: Define challenges for mutex ordering**
- [ ] **Step 3: Define challenges for Flow cancellation safety**
- [ ] **Step 4: Define challenges for semaphore effectiveness**
- [ ] **Step 5: Validate JSON schema, commit**

### Task 6.2: Create Performance Optimization Challenge Bank

**Files:**
- Create: `Challenges/banks/yole/performance-optimization-challenges.json`

- [ ] **Step 1: Define challenges for lazy initialization verification**
- [ ] **Step 2: Define challenges for cache effectiveness**
- [ ] **Step 3: Define challenges for non-blocking guarantees**
- [ ] **Step 4: Validate JSON schema, commit**

### Task 6.3: Create Security Scanning Challenge Bank

**Files:**
- Create: `Challenges/banks/yole/security-scanning-validation-challenges.json`

- [ ] **Step 1: Define challenges for Detekt zero-issues**
- [ ] **Step 2: Define challenges for OWASP clean bill**
- [ ] **Step 3: Define challenges for Gitleaks clean scan**
- [ ] **Step 4: Validate JSON schema, commit**

### Task 6.4: Create Test Coverage Challenge Bank

**Files:**
- Modify: `Challenges/banks/yole/test-coverage.json`

- [ ] **Step 1: Update coverage thresholds to new maximums**
- [ ] **Step 2: Add per-format coverage challenges**
- [ ] **Step 3: Add per-protocol coverage challenges**
- [ ] **Step 4: Validate JSON schema, commit**

### Task 6.5: Run All Challenges

- [ ] **Step 1: Run `./gradlew runChallenges`**
- [ ] **Step 2: Verify all banks pass**
- [ ] **Step 3: Fix any failures**
- [ ] **Step 4: Commit**

---

## Phase 7: Documentation — Full Completion

**Goal:** Update every documentation artifact to reflect current state; no stale information anywhere.

### Task 7.1: Rewrite FORMAT_DOCUMENTATION.md

**Files:**
- Modify: `FORMAT_DOCUMENTATION.md` (root)

- [ ] **Step 1: Remove all references to legacy platform-specific Java structure**
- [ ] **Step 2: Rewrite to reflect KMP `shared/src/commonMain/kotlin/digital/vasic/yole/format/` structure**
- [ ] **Step 3: Document all 17 format parsers with their capabilities**
- [ ] **Step 4: Add code examples for each format**
- [ ] **Step 5: Commit**

### Task 7.2: Update TESTING_GUIDELINES.md to v2.0

**Files:**
- Modify: `TESTING_GUIDELINES.md` (root)

- [ ] **Step 1: Update version to 2.0, date to 2026-03-17**
- [ ] **Step 2: Update all coverage numbers to current actuals**
- [ ] **Step 3: Add all 16 test types with descriptions and locations**
- [ ] **Step 4: Add concurrency test guidelines**
- [ ] **Step 5: Add performance baseline test guidelines**
- [ ] **Step 6: Commit**

### Task 7.3: Update TESTING_STRATEGY.md

**Files:**
- Modify: `TESTING_STRATEGY.md` (root)

- [ ] **Step 1: Update test count from ~9,400 to actual new count**
- [ ] **Step 2: Add new test categories (concurrency safety, monitoring/metrics, OWASP security)**
- [ ] **Step 3: Update platform-specific test matrix**
- [ ] **Step 4: Commit**

### Task 7.4: Update ARCHITECTURE.md

**Files:**
- Modify: `ARCHITECTURE.md` (root)

- [ ] **Step 1: Add concurrency safety section (AtomicBoolean, semaphores, lazy init)**
- [ ] **Step 2: Add security scanning section**
- [ ] **Step 3: Update test counts and coverage metrics**
- [ ] **Step 4: Commit**

### Task 7.5: Update CLAUDE.md and AGENTS.md

**Files:**
- Modify: `CLAUDE.md` (root)
- Modify: `AGENTS.md` (root)

- [ ] **Step 1: Update test count in CLAUDE.md**
- [ ] **Step 2: Add concurrency patterns to Architecture section**
- [ ] **Step 3: Add semaphore and lazy init patterns**
- [ ] **Step 4: Update Known Issues if any resolved**
- [ ] **Step 5: Commit**

### Task 7.6: Update All docs/ Files

**Files:**
- Modify: `docs/CONCURRENCY_SAFETY.md` — add AtomicBoolean patterns, Flow cancellation patterns
- Modify: `docs/LOCK_ORDERING.md` — add new lock hierarchy entries
- Modify: `docs/PERFORMANCE_TUNING.md` — add semaphore tuning, lazy init benchmarks
- Modify: `docs/MONITORING_GUIDE.md` — add metrics collection test patterns
- Modify: `docs/SECURITY_SCANNING.md` — add SBOM generation, CodeQL config
- Modify: `docs/FORMAT_SUPPORT_MATRIX.md` — verify all 17 formats listed with current capabilities
- Modify: `docs/LAZY_LOADING.md` — add new lazy patterns
- Modify: `docs/BUILD_SYSTEM.md` — add SBOM, coverage gate

- [ ] **Step 1–8: Update each file with current information**
- [ ] **Step 9: Commit all docs updates**

### Task 7.7: Create Concurrency Safety Architecture Diagram

**Files:**
- Create: `docs/diagrams/concurrency-safety.mmd`

- [ ] **Step 1: Create Mermaid diagram showing lock ordering, AtomicBoolean usage, semaphore flows**
- [ ] **Step 2: Commit**

### Task 7.8: Create Security Scanning Architecture Diagram

**Files:**
- Create: `docs/diagrams/security-scanning-pipeline.mmd`

- [ ] **Step 1: Create Mermaid diagram showing all security tools, their triggers, and report flows**
- [ ] **Step 2: Commit**

### Task 7.9: Archive Historical Session Notes

**Files:**
- Move 24 session/phase files from `docs/` to `docs/archive/`

- [ ] **Step 1: Move `docs/SESSION_*`, `docs/PHASE_*`, `docs/CURRENT_STATUS.md`, `docs/NEXT_STEPS*.md` to `docs/archive/`**
- [ ] **Step 2: Update `docs/README.md` index**
- [ ] **Step 3: Commit**

### Task 7.10: Expand KMP Module Documentation

**Files:**
- Modify CHANGELOG.md and CONTRIBUTING.md in all 10 extracted KMP modules (sibling directories)

- [ ] **Step 1: Expand each CHANGELOG.md with version history, breaking changes, migration notes**
- [ ] **Step 2: Expand each CONTRIBUTING.md with build instructions, test commands, coding standards**
- [ ] **Step 3: Commit per module**

---

## Phase 8: User Manuals — Step-by-Step Platform Guides

**Goal:** Create comprehensive step-by-step user manuals for every platform.

### Task 8.1: Create Android User Manual

**Files:**
- Create: `docs/user-guide/android-user-manual.md`

- [ ] **Step 1: Write installation guide (Google Play, APK sideload)**
- [ ] **Step 2: Write getting started walkthrough**
- [ ] **Step 3: Write format-by-format usage guides for all 17 formats**
- [ ] **Step 4: Write cloud storage setup (Dropbox, Google Drive, OneDrive, WebDAV, Git)**
- [ ] **Step 5: Write network storage setup (FTP, SFTP, SMB)**
- [ ] **Step 6: Write settings and customization guide**
- [ ] **Step 7: Write troubleshooting section**
- [ ] **Step 8: Commit**

### Task 8.2: Create Desktop User Manual

**Files:**
- Create: `docs/user-guide/desktop-user-manual.md`

- [ ] **Step 1: Write installation guide (Windows, macOS, Linux)**
- [ ] **Step 2: Write getting started walkthrough**
- [ ] **Step 3: Write keyboard shortcuts reference**
- [ ] **Step 4: Write format and storage sections**
- [ ] **Step 5: Write theme customization guide**
- [ ] **Step 6: Commit**

### Task 8.3: Create Web (Wasm) User Manual

**Files:**
- Create: `docs/user-guide/web-user-manual.md`

- [ ] **Step 1: Write PWA installation guide (all browsers)**
- [ ] **Step 2: Write getting started walkthrough**
- [ ] **Step 3: Write platform limitations section**
- [ ] **Step 4: Write offline usage guide**
- [ ] **Step 5: Commit**

### Task 8.4: Create Developer Quick Start Manual

**Files:**
- Modify: `QUICK_START.md`

- [ ] **Step 1: Update with current build commands**
- [ ] **Step 2: Add container setup walkthrough**
- [ ] **Step 3: Add test execution walkthrough**
- [ ] **Step 4: Add security scanning walkthrough**
- [ ] **Step 5: Commit**

### Task 8.5: Create API Reference Manual

**Files:**
- Create: `docs/user-guide/api-reference.md`

- [ ] **Step 1: Document FormatRegistry API (detect, parse, parseWithCache)**
- [ ] **Step 2: Document TextParser API (parse, toHtml, validate)**
- [ ] **Step 3: Document NetworkStorageService API (all 8 protocols)**
- [ ] **Step 4: Document DocumentCache API**
- [ ] **Step 5: Document StyleSheets API**
- [ ] **Step 6: Commit**

---

## Phase 9: Video Course Extension

**Goal:** Add new episodes covering all new features, update existing episodes with current information.

### Task 9.1: Create Episode 22 — Concurrency Safety Patterns

**Files:**
- Create: `video-course/expert/22-concurrency-safety/script.md`

- [ ] **Step 1: Write intro section (why concurrency matters in KMP)**
- [ ] **Step 2: Write AtomicBoolean section with code examples**
- [ ] **Step 3: Write Mutex and Semaphore section**
- [ ] **Step 4: Write Flow cancellation safety section**
- [ ] **Step 5: Write testing concurrency section**
- [ ] **Step 6: Commit**

### Task 9.2: Create Episode 23 — Security Scanning Deep Dive

**Files:**
- Create: `video-course/expert/23-security-scanning-deep-dive/script.md`

- [ ] **Step 1: Write SonarQube setup and analysis walkthrough**
- [ ] **Step 2: Write Snyk dependency scanning walkthrough**
- [ ] **Step 3: Write Gitleaks secret detection walkthrough**
- [ ] **Step 4: Write OWASP Dependency Check walkthrough**
- [ ] **Step 5: Write interpreting results and fixing findings**
- [ ] **Step 6: Commit**

### Task 9.3: Create Episode 24 — Performance Optimization Masterclass

**Files:**
- Create: `video-course/expert/24-performance-optimization/script.md`

- [ ] **Step 1: Write lazy loading deep dive**
- [ ] **Step 2: Write cache optimization (DocumentCache, StyleSheets)**
- [ ] **Step 3: Write semaphore-based backpressure**
- [ ] **Step 4: Write performance regression testing**
- [ ] **Step 5: Write benchmarking with the benchmark suite**
- [ ] **Step 6: Commit**

### Task 9.4: Create Episode 25 — Complete Test Coverage Guide

**Files:**
- Create: `video-course/expert/25-complete-test-coverage/script.md`

- [ ] **Step 1: Write overview of all 16 test types**
- [ ] **Step 2: Write per-type examples and when to use each**
- [ ] **Step 3: Write Kover coverage analysis walkthrough**
- [ ] **Step 4: Write property-based testing deep dive**
- [ ] **Step 5: Write fuzz testing deep dive**
- [ ] **Step 6: Commit**

### Task 9.5: Update All Existing Video Course Scripts

**Files:**
- Modify all 21 existing scripts in `video-course/`

- [ ] **Step 1: Update test counts (from 9,400 to new total)**
- [ ] **Step 2: Update architecture references (add concurrency patterns)**
- [ ] **Step 3: Update security scanning references**
- [ ] **Step 4: Update challenge framework references (new banks)**
- [ ] **Step 5: Verify all code examples compile and run**
- [ ] **Step 6: Commit**

### Task 9.6: Update video-course/README.md

**Files:**
- Modify: `video-course/README.md`

- [ ] **Step 1: Add episodes 22-25 to the course catalog**
- [ ] **Step 2: Update learning path recommendations**
- [ ] **Step 3: Update prerequisites**
- [ ] **Step 4: Commit**

---

## Phase 10: Website Content Update

**Goal:** Update all 11 website pages with current metrics, features, and content.

### Task 10.1: Update Home Page

**Files:**
- Modify: `website/app/page.tsx`

- [ ] **Step 1: Update test count metric**
- [ ] **Step 2: Update feature highlights (concurrency safety, security scanning)**
- [ ] **Step 3: Add performance metrics section**
- [ ] **Step 4: Commit**

### Task 10.2: Update Architecture Page

**Files:**
- Modify: `website/app/architecture/page.tsx`

- [ ] **Step 1: Add concurrency safety patterns section**
- [ ] **Step 2: Add security scanning infrastructure section**
- [ ] **Step 3: Add performance optimization section**
- [ ] **Step 4: Update module count and test metrics**
- [ ] **Step 5: Commit**

### Task 10.3: Update Formats Page

**Files:**
- Modify: `website/app/formats/page.tsx`

- [ ] **Step 1: Verify all 17 formats listed with current test counts**
- [ ] **Step 2: Add test coverage percentages per format**
- [ ] **Step 3: Commit**

### Task 10.4: Update Docs Page

**Files:**
- Modify: `website/app/docs/page.tsx`

- [ ] **Step 1: Add links to new user manuals**
- [ ] **Step 2: Add links to security scanning docs**
- [ ] **Step 3: Add links to concurrency safety docs**
- [ ] **Step 4: Commit**

### Task 10.5: Update Video Course Page

**Files:**
- Modify: `website/app/video-course/page.tsx`

- [ ] **Step 1: Add episodes 22-25**
- [ ] **Step 2: Update episode descriptions**
- [ ] **Step 3: Add expert-level learning path**
- [ ] **Step 4: Commit**

### Task 10.6: Update Cloud Storage Page

**Files:**
- Modify: `website/app/cloud-storage/page.tsx`

- [ ] **Step 1: Add concurrency safety information per protocol**
- [ ] **Step 2: Add circuit breaker and connection limiter documentation**
- [ ] **Step 3: Update platform availability matrix**
- [ ] **Step 4: Commit**

### Task 10.7: Update All Remaining Pages

**Files:**
- Modify: `website/app/about/page.tsx`
- Modify: `website/app/changelog/page.tsx`
- Modify: `website/app/community/page.tsx`
- Modify: `website/app/download/page.tsx`
- Modify: `website/app/faq/page.tsx`

- [ ] **Step 1: Update about page with current project metrics**
- [ ] **Step 2: Update changelog page with latest entries**
- [ ] **Step 3: Update community page with contribution stats**
- [ ] **Step 4: Update download page with latest release info**
- [ ] **Step 5: Update FAQ with new questions (security scanning, concurrency, performance)**
- [ ] **Step 6: Commit**

### Task 10.8: Rebuild Website

- [ ] **Step 1: Run `cd website && npm run build`**
- [ ] **Step 2: Verify all pages render correctly**
- [ ] **Step 3: Commit built output**

---

## Phase 11: Final Verification & Validation

**Goal:** Run every test, challenge, scan, and verification to ensure 100% completion.

### Task 11.1: Run Full Desktop Test Suite

- [ ] **Step 1: `./gradlew :shared:desktopTest`**
- [ ] **Step 2: Verify ALL tests PASS, 0 failures**
- [ ] **Step 3: Record exact test count**

### Task 11.2: Run Full Challenge Suite

- [ ] **Step 1: `./gradlew runChallenges`**
- [ ] **Step 2: Verify all challenge banks pass**

### Task 11.3: Run Coverage Report

- [ ] **Step 1: `./gradlew :shared:desktopTest koverHtmlReport`**
- [ ] **Step 2: Verify coverage exceeds 70% line and branch**
- [ ] **Step 3: Document per-file coverage**

### Task 11.4: Run Detekt

- [ ] **Step 1: `./gradlew detekt`**
- [ ] **Step 2: Verify 0 issues (maxIssues: 0)**

### Task 11.5: Run Security Scan

- [ ] **Step 1: `./scripts/run_security_scan.sh`**
- [ ] **Step 2: Verify no HIGH/CRITICAL findings**
- [ ] **Step 3: Document results**

### Task 11.6: Run OWASP Dependency Check

- [ ] **Step 1: `./gradlew dependencyCheckAnalyze`**
- [ ] **Step 2: Verify no CVSS >= 7.0 findings**

### Task 11.7: Verify No Disabled Tests

- [ ] **Step 1: `grep -r "@Ignore\|@Disabled\|@Skip" shared/src/`**
- [ ] **Step 2: Verify 0 matches**

### Task 11.8: Verify No Dead Code (TODO/FIXME/HACK/STUB)

- [ ] **Step 1: Audit all remaining TODO comments — ensure they are documented enhancement opportunities, not broken functionality**
- [ ] **Step 2: Verify no FIXME, HACK, STUB, PLACEHOLDER in production code**

### Task 11.9: Generate Final Report

**Files:**
- Create: `docs/COMPLETION_REPORT_2026-03-17.md`

- [ ] **Step 1: Write executive summary of all changes**
- [ ] **Step 2: Write test metrics (before/after)**
- [ ] **Step 3: Write security scan results**
- [ ] **Step 4: Write concurrency fixes summary**
- [ ] **Step 5: Write documentation inventory**
- [ ] **Step 6: Write video course inventory**
- [ ] **Step 7: Write website pages inventory**
- [ ] **Step 8: Write challenge banks inventory**
- [ ] **Step 9: Commit**

---

# PART 3: PHASE DEPENDENCY GRAPH

```
Phase 1 (Concurrency) ──────────────────────────────────────────┐
Phase 2 (Dead Code) ────────────────────────────────────────────┤
                                                                 ├── Phase 3 (Tests) ──┐
                                                                 │                      │
Phase 4 (Security Infra) ──────────────────────────────────────┤                      │
Phase 5 (Performance) ─────────────────────────────────────────┤                      │
                                                                 │                      ├── Phase 11 (Final)
Phase 6 (Challenges) depends on Phase 1, 3, 4, 5 ─────────────┤                      │
Phase 7 (Documentation) depends on all above ──────────────────┤                      │
Phase 8 (User Manuals) depends on Phase 7 ─────────────────────┤                      │
Phase 9 (Video Courses) depends on Phase 7 ────────────────────┤                      │
Phase 10 (Website) depends on Phase 7, 9 ──────────────────────┘                      │
                                                                                        │
Phase 11 (Final Verification) depends on ALL above ────────────────────────────────────┘
```

**Parallelizable phases:**
- Phase 1 + Phase 2 + Phase 4 (independent infrastructure work)
- Phase 7 + Phase 8 + Phase 9 (independent documentation work, after content phases)

**Sequential dependencies:**
- Phase 3 depends on Phase 1 & 2 (tests verify the fixes)
- Phase 5 depends on Phase 1 (performance relies on correct concurrency)
- Phase 6 depends on Phase 1, 3, 4, 5 (challenges validate everything)
- Phase 10 depends on Phase 7 & 9 (website references docs and videos)
- Phase 11 depends on ALL (final verification)

---

# PART 4: ESTIMATED SCOPE

| Phase | New Files | Modified Files | New Tests | Est. Lines Changed |
|-------|-----------|---------------|-----------|-------------------|
| Phase 1: Concurrency Safety | 3-5 | 15 | 150+ | 800-1,200 |
| Phase 2: Dead Code/Platform | 1-2 | 8 | 20+ | 400-600 |
| Phase 3: Test Coverage | 15-20 | 5 | 500+ | 6,000-8,000 |
| Phase 4: Security Infra | 3-4 | 6 | 0 | 300-500 |
| Phase 5: Performance | 2-3 | 5 | 80+ | 600-800 |
| Phase 6: Challenges | 3-4 | 1 | 0 | 400-600 |
| Phase 7: Documentation | 2-3 | 15+ | 0 | 2,000-3,000 |
| Phase 8: User Manuals | 5 | 1 | 0 | 3,000-5,000 |
| Phase 9: Video Courses | 4-5 | 21 | 0 | 2,000-3,000 |
| Phase 10: Website | 0 | 11 | 0 | 1,000-1,500 |
| Phase 11: Final Verification | 1 | 0 | 0 | 500-800 |
| **TOTAL** | **~42** | **~88** | **~750+** | **~17,000-25,000** |

---

# PART 5: CONSTRAINTS COMPLIANCE CHECKLIST

- [ ] **GitSpec**: All commits use Conventional Commits format (`feat:`, `fix:`, `docs:`, `test:`, `chore:`)
- [ ] **GitSpec**: No force pushes to master
- [ ] **GitSpec**: All tests pass before each commit
- [ ] **CLAUDE.md**: Release builds in containers
- [ ] **CLAUDE.md**: No tests removed, disabled, skipped, or broken
- [ ] **CLAUDE.md**: All fixes covered by all supported test types
- [ ] **CLAUDE.md**: SPDX headers on all new files
- [ ] **AGENTS.md**: 70%+ code coverage maintained
- [ ] **AGENTS.md**: Thread safety maintained across all packages
- [ ] **AGENTS.md**: No circular dependencies introduced
- [ ] **Non-Interactive**: No sudo/root prompts in any script
- [ ] **Non-Breaking**: All existing 6,695+ tests continue to pass after every change
