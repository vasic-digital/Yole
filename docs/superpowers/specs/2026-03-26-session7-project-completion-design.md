# Session 7: Complete Project Completion — Design Specification

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring every module, application, library, test, and documentation artifact to 100% completion — zero unfinished features, zero dead code, zero concurrency hazards, zero security findings, maximum test coverage across ALL test types, complete documentation, updated video courses, and a fully current website.

**Architecture:** Fix-first approach — resolve all concurrency/safety issues and dead code before expanding tests, then layer on security scanning, documentation, and content updates. Each phase produces a shippable, non-breaking increment verified by the full test suite.

**Tech Stack:** Kotlin 2.0.20 (KMP), Compose Multiplatform 1.7.3, Ktor 3.0.2, Kotest 5.9.1, MockK 1.13.13, Docker/Podman, SonarQube Community, Snyk, Detekt 1.23.7, Gitleaks, OWASP Dependency Check 11.1.1, Go 1.24+ (Challenges/Containers/HelixQA), Next.js 14 (website)

---

# PART 1: COMPREHENSIVE AUDIT REPORT (as of 2026-03-26)

## 1.1 Current Project State

| Metric | Value |
|--------|-------|
| Desktop tests passing | 8,347 (0 failures, 277 test classes) |
| Total tests (all platforms + Go) | ~10,000+ |
| Test files | 251 (230 commonTest, 17 desktopTest, 4 wasmJsTest) |
| Source files (commonMain) | 73 |
| Platform-specific source files | 37 (9 android, 9 desktop, 10 ios, 9 wasm) |
| Text format parsers | 17 |
| Network protocol services | 8 |
| Extracted KMP modules | 10 (9 in settings.gradle.kts + Formatters-KMP) |
| Go submodules | 3 (Challenges 109 tests, Containers 116 tests, HelixQA 41 tests) |
| Sibling Go modules | 5 (DocProcessor, LLMOrchestrator, VisionEngine, LLMProvider, LLMsVerifier) |
| Video course episodes | 31 |
| Website pages | 11 |
| Challenge banks | 27 JSON + 2 YAML |
| Documentation files | 228 markdown in docs/, 98,000+ lines total |
| Disabled/skipped tests | 0 |
| CI/CD workflows | 0 (mandatory: no CI/CD pipelines per CLAUDE.md) |

## 1.2 Concurrency & Safety Issues (15 items)

### 1.2.1 CRITICAL Severity (2 items)

**C1: Unprotected `_isConnected` read in `isOnline` property (ALL 8 protocol services)**
- Files: DropboxService.kt:109, FtpService.kt:87, GitService.kt:117, GoogleDriveService.kt:109, OneDriveService.kt:109, SftpService.kt:81, SmbService.kt:128, WebDavService.kt:103
- Problem: `override val isOnline: Boolean get() = _isConnected` reads without `stateMutex`. Property can return stale value while disconnect is in progress on another coroutine.
- Impact: UI shows "connected" while service is disconnecting; check-then-act race in callers.
- Fix: Add `@Volatile` to `_isConnected` in all 8 services. This is safe because the field is only written under `stateMutex` (single-writer) and read from non-suspending property getter (multiple readers).

**C2: Unprotected `_rootPath`/`_rootFolderId` reads (ALL 8 protocol services)**
- Files: DropboxService.kt:98, FtpService.kt:63, GoogleDriveService.kt:98, OneDriveService.kt:99, SftpService.kt:61, SmbService.kt:68, others
- Problem: Root path/folder ID set once during connect() under stateMutex but read in every file operation method without any synchronization.
- Impact: File operations during connect() could see null/empty root path.
- Fix: Add `@Volatile` to `_rootPath` and `_rootFolderId` in all services. Safe because write-once-read-many pattern.

### 1.2.2 HIGH Severity (3 items)

**C3: Lazy httpClient isInitialized() check-then-close race (3 cloud services)**
- Files: DropboxService.kt:239, GoogleDriveService.kt (similar), OneDriveService.kt (similar)
- Problem: `if (_httpClientLazy.isInitialized()) { httpClient.close() }` is non-atomic. Between check and close, another thread could trigger lazy initialization.
- Fix: Wrap in try-catch; close unconditionally if initialized. Kotlin's lazy SYNCHRONIZED mode prevents double-init, so this is a documentation+safety issue.

**C4: Unsynchronized `parseSemaphore` reassignment (FormatRegistry.kt:552)**
- Problem: `parseSemaphore = Semaphore(permits = permits)` can race with concurrent `parseSemaphore.withPermit` calls, causing permit leaks.
- Fix: Make `parseSemaphore` `@Volatile` and document that `configureParseConcurrency()` must be called before any concurrent parse operations, or protect with mutex.

**C5: Mutable collections accessed outside mutex (2 services)**
- Files: FtpService.kt:84 (pausedOperations), SftpService.kt:74 (virtualFileSystem)
- Fix: Audit and verify ALL accesses are within withLock blocks.

### 1.2.3 MEDIUM Severity (4 items)

**C6: Lock ordering violation in cancelOperation() (3 cloud services)**
- Current order: activeJobsMutex(7) -> pauseFlagsMutex(6) -> operationsMutex(3)
- Required order per LOCK_ORDERING.md: operationsMutex(3) -> pauseFlagsMutex(6) -> activeJobsMutex(7)
- Fix: Reorder mutex acquisitions. Sequential (not nested), so no deadlock currently, but violates convention.

**C7: Inconsistent @Volatile application (6 services)**
- GitService and WebDavService have `@Volatile` on `httpClientInitialized` but other services don't have it on equivalent fields.
- Fix: Apply `@Volatile` consistently to all read-mostly fields across all services.

**C8: Missing withTimeout on HTTP operations (5 HTTP-based services)**
- Fix: Configure Ktor HttpClient with install(HttpTimeout) { requestTimeoutMillis = 30_000; connectTimeoutMillis = 10_000 }.

**C9: Non-atomic removal across syncMutex + cacheMutex (3 cloud services)**
- Fix: Document that atomicity across both maps is not required (independent cleanup is acceptable).

### 1.2.4 LOW Severity (4 items)

**C10:** DocumentCache counters approximate (acceptable for metrics).
**C11:** SupervisorJob lifecycle — add cleanup documentation.
**C12:** initJob assignment race window — negligible in practice.
**C13:** Unclosed lazy properties — verify no resources held.

## 1.3 Dead Code & Stubs (8 items)

| ID | Type | Detail | Action |
|----|------|--------|--------|
| D1 | iOS stubs | 3 protocol clients, 32 methods | Document with KDoc, keep as intentional |
| D2 | Wasm stubs | 3 protocol clients, 36 methods | Document with KDoc, keep as intentional |
| D3 | Wasm TODO | SecureStorage localStorage | Document limitation, add IndexedDB TODO |
| D4 | Empty catches | 22 instances in resource cleanup | Add DEBUG-level logging |
| D5 | Placeholder test | IOSPlatformTests assertTrue(true) | Replace with real platform assertion |
| D6 | Incomplete screen | YoleApp.kt TODO screen state | Document as feature TODO |
| D7 | Unused enum | STUBBED tier in NetworkProtocolStatus | Remove |
| D8 | iOS encoding | IOSDocumentProvider fallback null | Fix with Latin-1 fallback |

## 1.4 Test Coverage Gaps

### Source files without direct tests (23 files):

**HIGH priority (complex logic):**
- CircuitBreaker.kt — isolated unit tests needed
- ConnectionLimiter.kt — isolated unit tests needed
- FormatRegistry.kt (581 LOC) — dedicated unit tests needed
- NetworkStorageError.kt (724 LOC) — error handling tests needed

**MEDIUM priority (parsers with only integration coverage):**
- PlaintextParser.kt, RestructuredTextParser.kt, WikitextParser.kt, TaskpaperParser.kt
- FtpProtocolClient.kt, SmbProtocolClient.kt, SshClient.kt

**LOW priority (simple models/utilities):**
- DocumentType.kt, OperationStatus.kt, OperationType.kt, SyncStatus.kt
- MetricsReporter.kt, MetricsSnapshot.kt, PlatformFileIOFactory.kt, Volatile.kt

### Test types taxonomy (all 16 types present):

| Type | Current Count | Expansion Target |
|------|--------------|-----------------|
| Unit | 1,437+ | +800 (untested files) |
| Integration | 71 | +150 (cross-module) |
| Stress | 263 | +200 (per-protocol, per-parser) |
| Supremacy/Edge-case | 12 | +50 (per-component) |
| Mock HTTP | 312 | +30 (edge cases) |
| Property-based | 19 | +80 (all parsers) |
| Contract | 89 | +20 (new components) |
| Security | 57 | +60 (OWASP, injection) |
| Performance | 144 | +40 (regression baselines) |
| Resilience | 86 | +30 (timeout, recovery) |
| Fuzz | 23 | +40 (per-format) |
| Snapshot | 46 | +20 (new formats) |
| Load | 22 | +30 (protocol load) |
| E2E | 102 | +20 (cross-cutting) |
| Accessibility | 230 | +10 (new components) |
| Non-blocking | 25 | +30 (network layer) |

## 1.5 Security Infrastructure

| Tool | Status | Gap |
|------|--------|-----|
| SonarQube | Container configured (docker-compose.yml) | Needs execution + findings analysis |
| Snyk | Container configured (docker-compose.yml) | Needs execution + findings analysis |
| Detekt | Active (maxIssues: 0) | 13 rules disabled |
| Gitleaks | Script configured | Working |
| OWASP Dep Check | Gradle plugin v11.1.1 | CVSS threshold 9.0 (too permissive) |
| .env files | 39+ API keys tracked in git | CRITICAL: remove from tracking |
| Build passwords | Hardcoded `yole123` in build.sh | Replace with env vars |

## 1.6 Documentation Gaps

| Gap | Severity |
|-----|----------|
| LLMProvider: missing README, ARCHITECTURE, API_REFERENCE | HIGH |
| LLMsVerifier: missing root ARCHITECTURE.md | MEDIUM |
| API KDoc not generated | MEDIUM |
| Test counts inconsistent (9,400 vs 10,000) across docs | LOW |
| README.md version (v2.15.1) vs CHANGELOG (v2.18.0) mismatch | LOW |
| Website video course page shows 26 episodes, 31 exist | LOW |
| Challenge bank TODOs in 3 JSON + 1 YAML files | LOW |

## 1.7 Build Infrastructure

| Gap | Detail |
|-----|--------|
| Formatters-KMP | Missing from settings.gradle.kts (only 9 of 10 modules) |
| ProGuard/R8 | Minification disabled for release (isMinifyEnabled = false) |
| SonarQube project version | Shows v2.15.2, should be v2.18.0+ |

---

# PART 2: DESIGN — 14-PHASE IMPLEMENTATION PLAN

## Constraints (CLAUDE.md, AGENTS.md, GitSpec)

1. **No CI/CD Pipelines** — All builds/tests run manually or via Makefile
2. **Never Remove/Disable Tests** — Fix root causes only
3. **Release Builds in Containers** — Docker/Podman via docker-compose
4. **Non-Interactive** — No sudo/root prompts; all container ops non-interactive
5. **Non-Breaking** — Every phase produces a shippable increment; full test suite passes after each
6. **JUnit4 runner** — `runBlocking<Unit> { }` (not `runTest`)
7. **MockK is JVM-only** — Only in desktopTest and androidUnitTest
8. **jvmTarget = "11"** — All JVM compilations
9. **SPDX headers** — Required on all new files
10. **70%+ coverage** — Enforced by Kover
11. **Zero Detekt violations** — maxIssues: 0

---

## Phase 1: Concurrency Safety & Race Condition Fixes

**Goal:** Eliminate all 15 identified concurrency issues.
**Scope:** ~15 source files modified, ~3 new test files, ~150 new tests
**Verification:** `./gradlew :shared:desktopTest` — 0 failures

### Task 1.1: Add @Volatile to all read-without-lock fields (C1, C2, C7)
- [ ] Add `@Volatile` to `_isConnected` in all 8 protocol services
- [ ] Add `@Volatile` to `_rootPath` in Dropbox, FTP, SFTP, SMB services
- [ ] Add `@Volatile` to `_rootFolderId` in GoogleDrive, OneDrive services
- [ ] Add `@Volatile` to `_httpClientAccessed` in Dropbox, GoogleDrive, OneDrive (consistency)
- [ ] Add `@Volatile` to `parseSemaphore` in FormatRegistry.kt (C4)

### Task 1.2: Fix lock ordering violations (C6)
- [ ] Reorder cancelOperation() in DropboxService: operationsMutex -> pauseFlagsMutex -> activeJobsMutex
- [ ] Reorder cancelOperation() in GoogleDriveService: same order
- [ ] Reorder cancelOperation() in OneDriveService: same order
- [ ] Apply same fix to pauseOperation() and resumeOperation() if they have the same issue

### Task 1.3: Add HTTP timeout configuration (C8)
- [ ] Add HttpTimeout plugin to HttpClientFactory for all platforms with defaults (connect: 10s, request: 30s, socket: 30s)
- [ ] Verify timeouts don't break existing tests

### Task 1.4: Verify mutable collection mutex protection (C5)
- [ ] Audit all accesses to `pausedOperations` in FtpService — verify within `pauseMutex.withLock`
- [ ] Audit all accesses to `virtualFileSystem` in SftpService — verify within `vfsMutex.withLock`
- [ ] Fix any unprotected accesses

### Task 1.5: Document and address low-severity issues (C9-C13)
- [ ] Add KDoc comments documenting that syncMutex/cacheMutex non-atomicity is acceptable (C9)
- [ ] Add KDoc documenting DocumentCache counter approximation (C10)
- [ ] Add KDoc documenting SupervisorJob cleanup expectations (C11)
- [ ] Verify initJob assignment is safe in practice (C12)
- [ ] Verify fileIO/oauth2Flow don't hold closeable resources (C13)

### Task 1.6: Write concurrency regression tests
- [ ] Create `ConcurrencyRaceConditionTests.kt` — concurrent connect/disconnect/listFiles stress
- [ ] Create `FormatRegistryConcurrencyTests.kt` — concurrent parse + semaphore reconfiguration
- [ ] Create `LockOrderingValidationTests.kt` — verify lock ordering compliance
- [ ] Create `TimeoutBehaviorTests.kt` — verify HTTP timeout triggers correctly

---

## Phase 2: Dead Code Elimination & Empty Catch Fixes

**Goal:** Zero dead code, zero empty catch blocks, zero placeholder tests.
**Scope:** ~25 files modified
**Verification:** `./gradlew :shared:desktopTest` — 0 failures

### Task 2.1: Add logging to empty catch blocks (D4)
- [ ] Add `// Resource cleanup — failure is non-fatal` comments + optional debug logging to all 22 empty catch blocks
- [ ] Android FtpProtocolClient (6 blocks), SmbProtocolClient (4 blocks)
- [ ] Desktop FtpProtocolClient (6 blocks), SmbProtocolClient (4 blocks)
- [ ] Wasm WebSecureStorage (1 block)
- [ ] Automation scripts (6 blocks — JavaScript, leave as-is with comment)

### Task 2.2: Remove unused code (D7)
- [ ] Remove `ImplementationTier.STUBBED` from NetworkProtocolStatus.kt

### Task 2.3: Fix placeholder code (D5, D8)
- [ ] Replace `assertTrue(true)` in IOSPlatformTests with real platform verification
- [ ] Fix IOSDocumentProvider.readFileWithEncodingDetection to fall back to ISO-8859-1 instead of null

### Task 2.4: Document intentional stubs (D1, D2, D3, D6)
- [ ] Add comprehensive KDoc to all iOS protocol stubs explaining platform limitation and future plan
- [ ] Add comprehensive KDoc to all Wasm protocol stubs explaining browser limitation and future plan
- [ ] Add KDoc to Wasm SecureStorage documenting localStorage limitation and IndexedDB plan
- [ ] Add code comment to YoleApp.kt documenting TODO screen as a planned feature

---

## Phase 3: Security Scanning Execution & Remediation

**Goal:** Execute all security scanners, analyze ALL findings, resolve everything.
**Scope:** Container orchestration + scan + remediation
**Verification:** Zero HIGH/CRITICAL findings remaining

### Task 3.1: Credential cleanup (S1, S2)
- [ ] Remove `.env` from git tracking: `git rm --cached .env HelixQA/.env`
- [ ] Verify `.gitignore` already covers `.env` (it does)
- [ ] Replace hardcoded `yole123` in docker/scripts/build.sh with `${KEYSTORE_PASSWORD:-changeit}` env var
- [ ] Note: API keys should be revoked by user separately (cannot do non-interactively)

### Task 3.2: Execute SonarQube analysis
- [ ] Start SonarQube: `podman compose --profile security up -d sonarqube` (wait for health check)
- [ ] Run setup script: `./docker/scripts/setup-sonarqube.sh`
- [ ] Execute analysis: `./gradlew sonar` or via sonar-scanner
- [ ] Collect and analyze findings
- [ ] Resolve all CRITICAL and HIGH findings
- [ ] Document remaining findings with justification

### Task 3.3: Execute Snyk scan
- [ ] Run Snyk: `podman compose --profile security run --rm snyk snyk test --all-projects`
- [ ] Analyze vulnerability report
- [ ] Update dependencies for any HIGH/CRITICAL vulnerabilities
- [ ] Update `.snyk` with any justified exemptions

### Task 3.4: Execute Detekt + OWASP
- [ ] Run: `./gradlew detekt` — verify 0 violations
- [ ] Lower OWASP CVSS threshold from 9.0 to 7.0 in build.gradle.kts
- [ ] Run: `./gradlew dependencyCheckAnalyze`
- [ ] Resolve any findings above threshold

### Task 3.5: Update SonarQube project version
- [ ] Update sonar-project.properties version from 2.15.2 to 2.19.0

---

## Phase 4: Test Coverage Maximum — Isolated Unit Tests

**Goal:** Direct test coverage for all 23 untested source files.
**Scope:** ~23 new test files, ~800+ new tests
**Verification:** `./gradlew :shared:desktopTest` — 0 failures, coverage increase verified

### Task 4.1: High-priority isolated unit tests
- [ ] Create `CircuitBreakerUnitTest.kt` — trip threshold, reset behavior, half-open state, concurrent trips
- [ ] Create `ConnectionLimiterUnitTest.kt` — permit acquire/release, exhaustion, timeout, concurrent limit
- [ ] Create `FormatRegistryUnitTest.kt` — lazy init, format detection, parser lookup, concurrent access
- [ ] Create `NetworkStorageErrorUnitTest.kt` — error categories, message formatting, serialization, nested errors

### Task 4.2: Parser isolated unit tests
- [ ] Create `PlaintextParserUnitTest.kt` — line counting, encoding, large files, empty input
- [ ] Create `RestructuredTextParserUnitTest.kt` — directives, roles, grid tables, edge cases
- [ ] Create `WikitextParserUnitTest.kt` — templates, links, categories, nested markup
- [ ] Create `TaskpaperParserUnitTest.kt` — projects, tasks, tags, search queries

### Task 4.3: Protocol client isolated tests (desktopTest — uses MockK)
- [ ] Create `FtpProtocolClientUnitTest.kt` — connection lifecycle, command responses, error handling
- [ ] Create `SmbProtocolClientUnitTest.kt` — share connection, authentication, file operations
- [ ] Create `SshClientUnitTest.kt` — key auth, channel management, error recovery

### Task 4.4: Model and utility tests
- [ ] Create `DocumentTypeTest.kt` — enum values, string conversion
- [ ] Create `OperationStatusTest.kt` — state transitions, serialization
- [ ] Create `OperationTypeTest.kt` — enum completeness
- [ ] Create `SyncStatusTest.kt` — status transitions
- [ ] Create `MetricsReporterTest.kt` — report generation, formatting, empty metrics
- [ ] Create `MetricsSnapshotTest.kt` — serialization, timestamp, data integrity
- [ ] Create `PlatformFileIOFactoryTest.kt` — factory creation, platform detection
- [ ] Create `TextFormatExtendedTest.kt` — detection patterns, extension matching, metadata

### Task 4.5: Coverage across ALL test types per new file
Each new test file must include test methods covering: unit, property-based, fuzz (random input), resilience (error injection), security (malicious input), edge-case/supremacy.

---

## Phase 5: Stress, Load & Responsiveness Tests

**Goal:** Prove system cannot be overloaded or made unresponsive.
**Scope:** ~8 new test files, ~200+ new tests
**Verification:** All stress tests pass within timeout limits

### Task 5.1: Per-protocol stress tests
- [ ] Create `PerProtocolStressTests.kt` — 100 concurrent operations per protocol, rapid connect/disconnect cycles, operation cancellation under load

### Task 5.2: Parser stress tests
- [ ] Create `ParserOverloadStressTests.kt` — 100 concurrent parses of each format, 10MB document parsing, alternating format detection

### Task 5.3: Cache and registry stress tests
- [ ] Create `DocumentCacheStressTests.kt` — concurrent get/put/evict, LRU correctness under load, memory pressure simulation
- [ ] Create `FormatRegistryLoadTests.kt` — concurrent format detection + parsing + cache access

### Task 5.4: Timeout and recovery tests
- [ ] Create `TimeoutRecoveryTests.kt` — verify hung operations killed by withTimeout, circuit breaker trip under timeout cascade, service recovery after timeout storm

### Task 5.5: Non-blocking verification
- [ ] Create `SystemNonBlockingTests.kt` — verify no public API blocks the calling thread, verify all I/O uses suspend functions, verify UI thread never blocked

### Task 5.6: Monitoring under stress
- [ ] Create `MetricsUnderStressTests.kt` — verify PerformanceMetrics collection accuracy during stress, verify MetricsReporter generates correct reports under load

---

## Phase 6: Integration & E2E Test Expansion

**Goal:** Cross-cutting tests validating full system integration.
**Scope:** ~6 new test files, ~150+ new tests

### Task 6.1: Cross-protocol integration
- [ ] Create `MultiProtocolIntegrationTests.kt` — simultaneous connections to multiple services, cross-service file operations, service failover scenarios

### Task 6.2: Full pipeline E2E
- [ ] Create `FormatPipelineE2ETests.kt` — detect -> parse -> cache -> HTML -> style pipeline for all 17 formats
- [ ] Create `NetworkPipelineE2ETests.kt` — connect -> list -> download -> parse -> render pipeline

### Task 6.3: Error recovery E2E
- [ ] Create `ErrorRecoveryE2ETests.kt` — circuit breaker trip -> recovery -> resume, network timeout -> retry -> success, concurrent error recovery

### Task 6.4: Platform-specific E2E (desktopTest)
- [ ] Create `DesktopE2ETests.kt` — file open -> detect format -> parse -> render -> save cycle

---

## Phase 7: Challenge Bank Completion

**Goal:** All challenge banks complete, no TODOs, comprehensive coverage.
**Scope:** ~10 files updated/created

### Task 7.1: Resolve existing TODOs
- [ ] Fix TODOs in format-detection.json, format-parsing.json, platform-coverage-challenges.json
- [ ] Fix TODO in HelixQA all-formats.yaml

### Task 7.2: Add new challenge banks
- [ ] Create `concurrency-safety-validation.json` — validates all race condition fixes
- [ ] Create `timeout-recovery-challenges.json` — validates timeout and recovery behavior
- [ ] Create `dead-code-elimination-challenges.json` — validates no dead code remains
- [ ] Update `security-scanning-validation.json` — add SonarQube/Snyk validation

### Task 7.3: Validate all banks
- [ ] Run ChallengeValidationTests — all banks parse correctly
- [ ] Verify challenge count matches test coverage

---

## Phase 8: Lazy Loading, Semaphore & Non-Blocking Optimization

**Goal:** Maximum responsiveness and resource efficiency.
**Scope:** ~12 files modified, ~4 new test files

### Task 8.1: Audit and add lazy loading
- [ ] Verify FormatRegistry.formats is lazy (already done)
- [ ] Verify StyleSheets cache is lazy (already done)
- [ ] Audit all singleton/object initializations for lazy loading opportunities
- [ ] Add lazy initialization to any remaining eager initializations

### Task 8.2: Semaphore tuning
- [ ] Verify parseSemaphore is thread-safe after Phase 1 fix
- [ ] Add semaphore-controlled concurrency to protocol service bulk operations
- [ ] Add configurable concurrency limits to DocumentCache

### Task 8.3: Non-blocking verification
- [ ] Verify all public APIs are non-blocking (supplement Phase 5 Task 5.5)
- [ ] Add `@NonBlocking` documentation annotations where applicable
- [ ] Create performance regression tests comparing lazy vs eager initialization

---

## Phase 9: Go Ecosystem Completion

**Goal:** All Go modules fully documented, all tests passing, all TODOs resolved.
**Scope:** ~6 doc files created, ~3 files updated

### Task 9.1: LLMProvider documentation
- [ ] Create README.md — module overview, package descriptions, usage examples
- [ ] Create ARCHITECTURE.md — system design, package interactions, provider pattern
- [ ] Create API_REFERENCE.md — public API documentation for all 9 packages

### Task 9.2: LLMsVerifier documentation
- [ ] Create root-level ARCHITECTURE.md — system overview of 44+ packages

### Task 9.3: Go module verification
- [ ] Run `go vet ./...` on all 8 Go modules (Challenges, Containers, HelixQA, DocProcessor, LLMOrchestrator, VisionEngine, LLMProvider, LLMsVerifier)
- [ ] Run `go test ./... -race -count=1` on all 8 modules
- [ ] Document any known flaky tests (pre-existing: TestStress_ConcurrentJWTRefresh, TestGenericPool_HealthyConnectionsSurvive)

---

## Phase 10: Documentation Blitz

**Goal:** Every file, feature, and API fully documented. All metrics synchronized.
**Scope:** ~30 files updated, ~5 new files

### Task 10.1: Generate API documentation
- [ ] Run `./gradlew :shared:dokkaHtml`
- [ ] Verify output in `shared/build/dokka/html/`

### Task 10.2: Synchronize metrics across all docs
- [ ] Count actual test numbers: `./gradlew :shared:desktopTest` output
- [ ] Update README.md: version, test count, feature count
- [ ] Update ARCHITECTURE.md: session 7 changes, concurrency fixes
- [ ] Update CONTRIBUTING.md: new test types, challenge procedures
- [ ] Update SECURITY.md: scanning results, resolved vulnerabilities
- [ ] Update CHANGELOG.md: session 7 entry with all phases
- [ ] Update AGENTS.md: if any new patterns or conventions added

### Task 10.3: Expand user manuals
- [ ] Update android-user-manual.md with latest features and counts
- [ ] Update desktop-user-manual.md with latest features and counts
- [ ] Update web-user-manual.md with latest features and counts
- [ ] Add step-by-step workflow descriptions for common tasks

### Task 10.4: Update performance and testing docs
- [ ] Update TESTING_GUIDE.md with new test types and counts
- [ ] Update performance docs with Phase 5 baseline results

---

## Phase 11: Video Course Extension

**Goal:** Video course current with all Session 7 work.
**Scope:** ~5 new scripts, ~3 updated scripts

### Task 11.1: Create new episode scripts
- [ ] Episode 32: "Concurrency Safety Patterns in KMP" — @Volatile, mutex ordering, withTimeout
- [ ] Episode 33: "Security Scanning Pipeline" — SonarQube, Snyk, Detekt, OWASP workflow
- [ ] Episode 34: "Stress Testing & Performance Monitoring" — stress tests, metrics, responsiveness
- [ ] Episode 35: "Challenge-Driven Development" — challenge bank framework, validation tests
- [ ] Episode 36: "Project Completion & Quality Gates" — full QA pipeline, zero-defect verification

### Task 11.2: Update existing scripts
- [ ] Update scripts referencing test counts (use current numbers)
- [ ] Update video-course/README.md with episodes 32-36

---

## Phase 12: Website Update

**Goal:** All pages reflect current state, metrics, and features.
**Scope:** ~8 TSX files updated

### Task 12.1: Update dynamic content
- [ ] Homepage: update test count stat, format count, protocol count
- [ ] Video course page: show all 36 episodes
- [ ] Changelog page: add v2.19.0 (Session 7)
- [ ] Architecture page: add concurrency patterns section
- [ ] Download page: verify platform status current

### Task 12.2: Content consistency
- [ ] Verify all version references are v2.19.0
- [ ] Verify all feature counts match actual implementation

---

## Phase 13: KMP Module Verification

**Goal:** All 10 extracted KMP modules verified working.
**Scope:** Verification + fixes

### Task 13.1: Verify composite builds
- [ ] Verify all 10 modules exist in sibling directories
- [ ] Check if Formatters-KMP should be added to settings.gradle.kts (currently 9)
- [ ] Verify each module compiles independently

### Task 13.2: Verify module documentation
- [ ] Check each module has substantive CHANGELOG.md (not placeholder)
- [ ] Check each module has substantive CONTRIBUTING.md (not placeholder)

---

## Phase 14: Final Verification & Comprehensive Regression

**Goal:** Zero failures, zero warnings, everything connected and documented.
**Scope:** Full regression suite

### Task 14.1: Kotlin test suite
- [ ] Run `./gradlew :shared:desktopTest` — 0 failures
- [ ] Run `./gradlew detekt` — 0 violations
- [ ] Record final test count

### Task 14.2: Go test suite
- [ ] Run `make challenge` — pass (note known flaky tests)
- [ ] Run `make helixqa-test` — pass

### Task 14.3: Documentation verification
- [ ] Verify all doc cross-references are accurate
- [ ] Verify all test counts in docs match actual numbers
- [ ] Verify website builds: `cd website && npm run build`

### Task 14.4: Final commit
- [ ] Comprehensive commit message with session 7 summary
- [ ] Update MEMORY.md project memory with session 7 results

---

# PART 3: SUCCESS CRITERIA

| Criterion | Metric |
|-----------|--------|
| Desktop tests | 0 failures, 9,000+ tests |
| Detekt violations | 0 |
| Disabled/skipped tests | 0 |
| Empty catch blocks | 0 (all have logging or documentation) |
| @Volatile on shared mutable fields | 100% |
| Lock ordering violations | 0 |
| HTTP timeout configuration | 100% of HTTP operations |
| Source files without direct tests | 0 |
| Security scan critical/high findings | 0 |
| .env files tracked in git | 0 |
| Go module documentation complete | 100% (README, ARCHITECTURE, API_REFERENCE) |
| Video course episodes | 36 |
| Website pages current | 100% |
| Challenge bank TODOs | 0 |
| Test types per component | All 16 types represented |

---

# PART 4: RISK MITIGATION

| Risk | Mitigation |
|------|-----------|
| SonarQube container fails to start | Use `podman compose` on ALT Linux; check memory (2GB required) |
| Snyk token not available | Run with `--severity-threshold=high` without auth for OSS scan |
| Tests break after concurrency fixes | Apply fixes incrementally, run full suite after each fix |
| Container OOM (exit 137) | Increase mem_limit in docker-compose.yml; split workload |
| Go flaky tests | Document as pre-existing; don't block on known flakes |
| AGP version mismatch | Use `:shared:desktopTest` (not Android-dependent) |
| Interactive prompts | All operations use non-interactive flags; no sudo required |
