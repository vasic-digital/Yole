# Comprehensive Project Completion — Design Specification

**Date:** 2026-03-19
**Status:** Approved (Rev 2 — post spec review corrections)
**Scope:** Full project completion across all modules, platforms, tests, documentation, and content

---

## 1. Executive Summary

This specification defines a 9-phase Build-Test-Document pipeline to bring the entire Yole project ecosystem to 100% completion. Every code change is immediately covered by tests and documented before the next phase begins. The plan addresses concurrency/safety issues (after re-auditing against Session 3 fixes already applied), security scanning infrastructure gaps, performance optimization, test coverage expansion to theoretical maximum, Go ecosystem fixes, legacy cleanup, comprehensive documentation, video course updates, and website content — leaving zero unfinished, undocumented, broken, or disabled components.

**IMPORTANT: Pre-Implementation Audit Requirement.** Session 3 (March 8, 2026) already applied fixes for: Flow CancellationException in 3 cloud services, SmbService connection guards, SftpService directory detection, DropboxService isLowOnSpace threshold, DocumentCache cooperative cancellation, and httpClient init patterns. Phase 1 MUST re-audit each proposed fix against current code before applying. Any already-fixed issue becomes a verification-only item (confirm fix is still in place + add regression test if not already covered).

**Constraints (Non-Negotiable):**
- No CI/CD pipelines (CLAUDE.md mandate)
- All tests use `runBlocking<Unit>` (JUnit4)
- No MockK in commonTest/wasmJsTest
- jvmTarget = "11" everywhere
- SPDX headers on all new files
- No interactive processes (no sudo/root prompts)
- CancellationException always rethrown
- Lock ordering respected (docs/LOCK_ORDERING.md)
- No test may be removed, disabled, or left broken
- No `kotlinx-coroutines-test` in commonTest (no WASM variant) — use `runBlocking<Unit>` + manual coroutine control
- All new commonTest files must compile for ALL targets (Android, Desktop, iOS, Wasm) — no MockK, no JVM-only APIs
- Release builds MUST run in containers (Docker/Podman) per CLAUDE.md
- Build naming: `Yole-{Platform}-{Version}-{Variant}-{VersionCodeDotted}`

---

## 2. Architecture Overview

### Pipeline Structure

Each phase follows the Fix → Test → Document pattern:

```
Phase N:
  Stage A: Code changes (fixes, new features, optimizations)
  Stage B: Tests (all applicable test types for every change)
  Stage C: Documentation (KDoc, markdown, diagrams)
  ↓ Verification checkpoint
Phase N+1: ...
```

### Phase Dependency Graph

```
Phase 1 (Concurrency) ──→ Phase 2 (Security) ──→ Phase 3 (Performance)
                                                         │
Phase 4 (Test Coverage) ←────────────────────────────────┘
         │
Phase 5 (Go Ecosystem) ──→ Phase 6 (Legacy Cleanup)
                                    │
Phase 7 (Documentation) ←──────────┘
         │
Phase 8 (Content & Media) ──→ Phase 9 (Final Verification)
```

### Scope Summary

| Phase | Name | New Files | Modified Files | New Tests |
|-------|------|-----------|---------------|-----------|
| 1 | Concurrency & Safety Foundation | ~12 | ~20 | ~200 |
| 2 | Security Hardening | ~12 | ~12 | ~150 |
| 3 | Performance & Optimization | ~18 | ~15 | ~200 |
| 4 | Test Coverage Maximum | ~188 | ~10 | ~4,750 |
| 5 | Go Ecosystem Completion | ~8 | ~12 | ~160 (Go) |
| 6 | Legacy Cleanup & Build Fixes | ~2 | ~8 | ~20 |
| 7 | Documentation Blitz | ~18 | ~40 | 0 |
| 8 | Content & Media | ~10 | ~30 | 0 |
| 9 | Final Verification | ~3 | ~5 | 0 |
| **Total** | | **~271** | **~152** | **~5,480** |

**Grand total tests after completion: ~21,000+ (All Kotlin ~14,150 + All Go ~6,872)**

Note on Kotlin test distribution: The ~14,150 total spans commonTest (~12,500), desktopTest (~800), androidUnitTest (~400), wasmJsTest (~250), iosTest (~200). The `./gradlew :shared:desktopTest` command runs commonTest + desktopTest only (~13,300). All new Phase 4 format/protocol test files go in commonTest (cross-platform, no MockK). Platform-specific tests go in their respective source sets.

---

## 3. Phase 1: Concurrency & Safety Foundation

**Goal:** Eliminate all critical and high-severity concurrency bugs, memory leak risks, and race conditions.

**PRE-IMPLEMENTATION STEP (MANDATORY):** Before applying ANY fix, audit the current code to check if Session 3 already resolved it. For each issue:
1. Read the current source file at the specified location
2. If already fixed → mark as "VERIFIED" and add regression test only
3. If partially fixed → apply remaining fix + test
4. If not fixed → apply full fix + test

### 3.1 Critical Fixes (5 issues — some may already be resolved by Session 3)

| # | File | Issue | Fix | Session 3 Status |
|---|------|-------|-----|-----------------|
| 1 | `DropboxService.kt` | `getRecentChanges()` — CancellationException caught after emit | Move CancellationException rethrow BEFORE any `emit()` in catch block | LIKELY FIXED — verify and add regression test |
| 2 | `GoogleDriveService.kt` | Same CancellationException pattern | Same fix | LIKELY FIXED — verify and add regression test |
| 3 | `OneDriveService.kt` | Same CancellationException pattern | Same fix | LIKELY FIXED — verify and add regression test |
| 4 | `SmbService.kt` | `deleteFile()` — isConnected check outside all mutex locks | Acquire `stateMutex` before connection check, hold through operation | LIKELY FIXED — verify connection guard and add regression test |
| 5 | `SftpService.kt` | `exists()` — calls `getFileInfo()` without VFS initialization check | Add `ensureInitialized()` guard before VFS access | PARTIALLY FIXED (trailing slash) — verify exists() path |

### 3.2 High-Severity Fixes (5 issues)

| # | File | Issue | Fix | Session 3 Status |
|---|------|-------|-----|-----------------|
| 6 | `WebDavService.kt` | `httpClientInitialized` boolean not mutex-protected (TOCTOU) | Replace with `::httpClient.isInitialized` atomic check under `stateMutex` | POSSIBLY FIXED — `_httpClientAccessed` pattern may be removed; verify current code |
| 7 | `GitService.kt` | Same httpClientInitialized TOCTOU | Same fix | POSSIBLY FIXED — verify current code |
| 8 | `FtpService.kt` | `disconnect()` — activeJobs cleared then cancelled outside lock (job leak) | Keep lock held through cancellation OR set disconnecting flag | NEEDS VERIFICATION |
| 9 | `SmbService.kt` | `getFileInfo()` returns synthesized NetworkDocument without lock | Hold lock through construction and return | LIKELY FIXED — verify connection guard |
| 10 | `DropboxService.kt` | `isLowOnSpace` uses `>= 0.9` (off-by-one) | Align with `NetworkStorage.kt` interface default which uses `> 0.9` | LIKELY FIXED — verify alignment between DropboxService and interface |

### 3.3 Medium-Severity Fixes (4 issues)

| # | File | Issue | Fix |
|---|------|-------|-----|
| 11 | `AuthTokenManager.kt` | Nested mutex deadlock risk | Merge into single mutex OR initialize storage eagerly |
| 12 | `DocumentCache.kt` | `yield()` before lock — cancelled ops continue | Move `yield()` inside `mutex.withLock {}` — LIKELY FIXED in Session 3, verify |
| 13 | `StyleSheets.kt` | `platformSynchronized` used for cache access | **KEEP platformSynchronized** — operations are fast in-memory map lookups; converting to suspend Mutex would break the non-suspend API surface and ripple through all callers. Instead: document why `platformSynchronized` is safe here (fast, non-blocking, no I/O). Only convert if profiling shows contention |
| 14 | `DesktopSecureStorage.kt` | Cache fields lack `@Volatile` | Add `@Volatile` to shared mutable fields |

### 3.4 Low-Severity Fixes (3 issues)

| # | File | Issue | Fix |
|---|------|-------|-----|
| 15 | `FormatRegistry.kt` | `parseSemaphore` hardcoded to 4 | Platform-aware default |
| 16 | `NetworkStorageConfigService.kt` | `configuredServices` map without sync | Protect with mutex |
| 17 | `SmbService.kt` | `listFiles()` Flow error propagation | Add `.catch {}` with CancellationException rethrow |

### 3.5 Tests (~200 new)

12 new test files covering all 17 fixes:
- `CancellationSafetyTests.kt` — Flow cancellation in 3 cloud services
- `SmbServiceConcurrencyTests.kt` — deleteFile, getFileInfo, listFiles
- `SftpServiceInitializationTests.kt` — exists() without VFS init
- `HttpClientLifecycleTests.kt` — TOCTOU in WebDAV and Git
- `FtpDisconnectSafetyTests.kt` — job leak during disconnect
- `AuthTokenManagerDeadlockTests.kt` — nested mutex
- `DocumentCacheCancellationTests.kt` — yield placement
- `StyleSheetsSynchronizationTests.kt` — platformSynchronized safety validation
- `DesktopSecureStorageVolatileTests.kt` — @Volatile (desktopTest)
- `FormatRegistrySemaphoreTests.kt` — configurable semaphore
- `ConfigServiceSyncTests.kt` — map synchronization
- `QuotaThresholdTests.kt` — isLowOnSpace threshold

Each file includes: unit, stress (100+ concurrent coroutines), regression, non-blocking validation.

### 3.6 Documentation

- Update `docs/LOCK_ORDERING.md` — AuthTokenManager mutex merge, new lock patterns
- Update `docs/CONCURRENCY_SAFETY.md` — before/after code for each fix
- KDoc on all 17 modified files — thread-safety guarantees, lock requirements

---

## 4. Phase 2: Security Hardening

**Goal:** Complete security scanning infrastructure, implement missing SecureStorage, fix security code gaps.

### 4.1 Security Scanning Infrastructure

| # | Task | Details |
|---|------|---------|
| 1 | Verify Docker/Podman Compose services | `docker compose --profile security up -d` (no sudo) |
| 2 | Run SonarQube analysis | Against localhost:9000 |
| 3 | Run Snyk scan | Via container with SNYK_TOKEN |
| 4 | Run Detekt | `./gradlew detekt` — must pass maxIssues=0 |
| 5 | Run Gitleaks | `gitleaks detect --source . --config .gitleaks.toml` |
| 6 | Run OWASP Dependency Check | `./gradlew dependencyCheckAnalyze` — CVSS 7.0 |
| 7 | Generate CycloneDX SBOM | `./gradlew cyclonedxBom` |
| 8 | Analyze and resolve all findings | Triage by severity, fix or suppress with rationale |

### 4.2 SecureStorage Platform Implementations

| Platform | Implementation |
|----------|----------------|
| iOS | Keychain Services: `SecItemAdd`, `SecItemCopyMatching`, `SecItemDelete` |
| Wasm | Web Crypto API (AES-GCM) + localStorage hybrid |

### 4.3 Security Code Fixes

- Certificate pinning configuration (optional, configurable) for Ktor HttpClient
- HTTPS enforcement for cloud protocols (HTTP allowed for local with user opt-in)
- `.snyk` policy with documented suppressions
- `docker-compose.yml` SonarQube non-root user, 3GB memory
- `SecurityEventLogger` interface — structured audit trail for auth failures, path traversal blocks

### 4.4 Tests (~150 new)

10 new test files: SecureStorage iOS/Wasm roundtrip, certificate pinning, HTTPS enforcement, SecurityEventLogger, deep path traversal fuzz, OAuth edge cases, credential storage, injection prevention, scanning validation.

### 4.5 Documentation

- Update `docs/SECURITY_SCANNING.md`, `docs/SECURITY.md`, `SECURITY.md`
- New: `docs/SECURITY_EVENT_LOGGING.md`, `docs/SBOM_GUIDE.md`
- KDoc on all security-related files

---

## 5. Phase 3: Performance & Optimization

**Goal:** Maximize lazy loading, introduce semaphore mechanisms, ensure non-blocking operations, create monitoring/metrics infrastructure.

### 5.1 Lazy Loading Expansion

- Lazy per-format parser instantiation (defer until format first requested)
- Lazy protocol service instantiation in NetworkStorageService
- Lazy HttpClient creation for token refresh in AuthTokenManager
- Lazy internal caches in all 8 protocol services
- Tiered DocumentCache: hot (in-memory) + cold (LRU eviction)

### 5.2 Semaphore Mechanisms

- Platform-aware `parseSemaphore`: Android=2, Desktop=2-8, Wasm=1, iOS=2
- `cacheSemaphore` in DocumentCache
- Global `networkSemaphore` across all protocols (default 16)
- `cssSemaphore(2)` in StyleSheets
- `ServiceSemaphore` for cross-service host coordination

### 5.3 Non-Blocking Mechanisms

- All CSS operations via `withContext(Dispatchers.Default)`
- TextParser HTML generation as suspend function
- Parallel format detection with async + parseSemaphore
- All file I/O wrapped in `withContext(Dispatchers.IO)` with `ensureActive()` checkpoints
- Background LRU eviction via `launch(Dispatchers.Default)`
- `ParsedDocument.toHtml()` as suspend with background pre-generation

### 5.4 Monitoring & Metrics Infrastructure

6 new files in `shared/.../monitoring/`:
- `PerformanceMetrics.kt` — AtomicLong counters + StateFlow observation
- `MetricsSnapshot.kt` — JSON-serializable point-in-time snapshot
- `MetricsReporter.kt` — Periodic logging (Logcat/stderr/console)
- `CacheMetrics.kt` — Per-cache hits, misses, evictions, size
- `NetworkMetrics.kt` — Per-protocol latency p50/p95/p99, error rate
- `ConcurrencyMetrics.kt` — Semaphore wait times, mutex contention

### 5.5 Tests (~200 new)

16 new test files: lazy parser init, tiered cache, lazy services, platform semaphores, global network semaphore, non-blocking parsing, async format detection, background eviction, all 6 metrics files, responsiveness stress (1000+ concurrent ops), memory leak detection (10K docs), overload resilience.

### 5.6 Documentation

- Rewrite `docs/LAZY_LOADING.md`
- Extend `docs/PERFORMANCE_TUNING.md`
- New: `docs/MONITORING_METRICS_REFERENCE.md`, `docs/NON_BLOCKING_ARCHITECTURE.md`
- New diagram: `docs/diagrams/performance-monitoring.mmd`

---

## 6. Phase 4: Test Coverage Maximum

**Goal:** Fill every test type gap for every component. 16 test types × all components.

### 6.1 Supported Test Types (16)

Unit, Integration, Stress, Fuzz, Snapshot, Load, E2E, Performance, Accessibility, Security, Resilience, Property-based, Contract, Non-blocking, Supremacy/Edge-case, Mock HTTP.

### 6.2 Format Parser Expansion (102 new test files)

17 formats × 6 missing test types (Security, Property-based, Contract, Resilience, Non-blocking, Supremacy) = 102 new files.

**CRITICAL CONSTRAINT:** All 102 format + 56 protocol test files go in `commonTest` (not `desktopTest`) for cross-platform validation. None may use MockK or `kotlinx-coroutines-test`. All must compile for ALL targets including Wasm. This nearly doubles the commonTest file count (~206 → ~400+). Monitor Wasm compilation times — if excessive, consider splitting into test-type-specific source sets.

Each format gets per-type tests:
- **Security**: XSS, injection, malicious content
- **Property-based**: Parse roundtrip invariants, HTML well-formedness
- **Contract**: TextParser interface compliance
- **Resilience**: Corrupt input, truncated files, extreme nesting
- **Non-blocking**: No blocking beyond threshold
- **Supremacy**: 0-byte, 100MB, single-char, unicode, RTL, emoji

### 6.3 Protocol Service Expansion (56 new test files)

8 protocols × 7 missing test types (Fuzz, Resilience, Property-based, Contract, Non-blocking, Supremacy, Load) = 56 new files.

### 6.4 Core Infrastructure (17 new test files)

FormatRegistry, DocumentCache, StyleSheets, CircuitBreaker, ConnectionLimiter, PathUtils, TextParser — fuzz, resilience, property-based, contract, non-blocking, supremacy, snapshot tests.

### 6.5 Auth/UI/Model/Utility (14 new test files)

AuthTokenManager, OAuth2Flow, Theme, Accessibility, Animations, Document, RateLimiter, LazyLoading, PlatformSync — filling all missing test type gaps.

### 6.6 Platform-Specific Expansion (10 new test files)

Android (3): stress, HTTP client, file IO. Desktop (2): stress, memory. Wasm (3): format parsing, non-blocking, memory. iOS (2): SecureStorage, platform stubs.

### 6.7 Challenge Bank Expansion (6 new banks)

- `property-based-validation.json` (20 challenges)
- `non-blocking-validation.json` (15 challenges)
- `contract-compliance.json` (18 challenges)
- `fuzz-resilience.json` (25 challenges)
- `load-endurance.json` (12 challenges)
- `supremacy-edge-cases.json` (30 challenges)

HelixQA banks: `coverage-validation.yaml` (40), `metrics-threshold.yaml` (20)

### 6.8 Test Count Projection

| Category | Current | New | Total |
|----------|---------|-----|-------|
| Format parsers | ~5,200 | ~1,800 | ~7,000 |
| Protocols | ~1,800 | ~1,200 | ~3,000 |
| Core infrastructure | ~600 | ~500 | ~1,100 |
| Auth/UI/Model/Utility | ~400 | ~400 | ~800 |
| Platform-specific | ~200 | ~300 | ~500 |
| Phase 1-3 tests | 0 | ~550 | ~550 |
| **Kotlin Total** | **~9,400** | **~4,750** | **~14,150** |

Challenge banks: 27 → 33. HelixQA banks: 1 → 3.

### 6.9 Documentation

- Rewrite `docs/TESTING_STRATEGY.md` — all 16 types with examples
- Update `docs/TESTING_GUIDELINES.md` v2.0
- Extend `docs/TEST_IMPLEMENTATION_GUIDE.md`
- New: `docs/TEST_COVERAGE_MATRIX.md`, `docs/CHALLENGE_BANKS_REFERENCE.md`
- Update `CLAUDE.md` test counts

---

## 7. Phase 5: Go Ecosystem Completion

**Goal:** Fix all LLMProvider failures, complete HelixQA production wiring, expand challenge integration.

### 7.1 LLMProvider Fixes (3 bugs)

| Provider | Issue | Fix |
|----------|-------|-----|
| Junie | DefaultModel/MaxTokens config wrong | Update defaults |
| Gemini | User-Agent mismatch | Update to `LLMProvider/1.0` |
| Zen | Panic on empty model list | Add nil/empty check |

### 7.2 Health Check Hardening

- Split tests: `_WithCredentials` (skip if no env) + `_WithoutCredentials` (verify graceful error)
- Structured error types: `ErrUnauthorized`, `ErrNotFound`, `ErrDNSResolutionFailed`
- DNS resolution timeout (5s)

### 7.3 HelixQA Production Wiring

- Complete `main.go` run subcommand with SessionCoordinator
- Wire DocProcessor, LLMOrchestrator, VisionEngine into coordinator
- Verify platform executors (ADB, Playwright, X11) with timeout handling
- Wire LLMProvider for real LLM calls with heuristic fallback
- Implement real evidence capture with graceful degradation

### 7.4 Challenge Integration

- Update 27 existing banks with current metrics
- New banks: `go-module-health.json` (15), `helixqa-pipeline.json` (20), `llmprovider-stability.json` (12)

### 7.5 Tests (~160 new Go tests)

8 new test files across HelixQA, LLMProvider, LLMOrchestrator, VisionEngine, DocProcessor.

### 7.6 Documentation

- Update README, CHANGELOG, API_REFERENCE for LLMProvider, HelixQA, Challenges
- New: `docs/GO_MODULES_REFERENCE.md`

**Go Total (approximate baseline from audit):**

| Module | Current | New | Total |
|--------|---------|-----|-------|
| Challenges | 2,070 | 0 | 2,070 |
| Containers | 1,713 | 0 | 1,713 |
| HelixQA | 654 | ~80 | ~734 |
| DocProcessor | 219 | ~15 | ~234 |
| LLMOrchestrator | 312 | ~15 | ~327 |
| VisionEngine | 308 | ~10 | ~318 |
| LLMProvider | 1,419 | ~57 | ~1,476 |
| **Go Total** | **~6,695** | **~177** | **~6,872 (0 failures)** |

Note: The Go baseline of ~6,695 is distinct from the Kotlin `desktopTest` count of 6,695 — this is a coincidence.

---

## 8. Phase 6: Legacy Cleanup & Build Fixes

**Goal:** Remove orphaned code, fix build inconsistencies, clean legacy modules.

### 8.1 Core Module

- Verify orphaned (zero references from shared/androidApp/desktopApp/webApp)
- Remove from `settings.gradle.kts`
- Archive to `archive/core/` with README

### 8.2 Commons Namespace

- Rename `net.gsantner.opoc.*` → `digital.vasic.yole.commons.*`
- Update all import statements in androidApp
- Add `@Deprecated` on classes with KMP replacements

### 8.3 Build Config Fixes

- Remove duplicate `kotlinx-serialization-json` dependency
- Replace hardcoded Ktor versions with version catalog references
- Move Room dependencies to `androidMain` only
- Add missing entries to `gradle/libs.versions.toml`

### 8.4 Build Warning Elimination

- Run `--warning-mode all`, fix all deprecation/configuration warnings
- Target: `--warning-mode fail` passes

### 8.5 Dependency Freshness

- Audit outdated dependencies, update safe patch/minor versions
- Document held-back dependencies with rationale

### 8.6 Tests (~20 new)

4 test files: namespace migration, build configuration, dependency resolution, deprecation.

### 8.7 Documentation

- Major update to `docs/LEGACY_MIGRATION.md`
- Update `CONTRIBUTING.md`, `ARCHITECTURE.md`
- New: `archive/core/README.md`

---

## 9. Phase 7: Documentation Blitz

**Goal:** 100% KDoc coverage, all markdown current, API docs generated, all diagrams accurate.

### 9.1 KDoc Coverage

All 68 commonMain + 33 platform-specific + 6 monitoring files get complete KDoc:
- Class-level: purpose, thread-safety, lifecycle
- Method-level: params, returns, throws, pre/post conditions
- Property-level: mutability, volatility, cache semantics

### 9.2 API Documentation

- Run `./gradlew :shared:dokkaHtml`
- Copy to `docs/api/html/`
- Update `docs/api/README.md`

### 9.3 Markdown Fixes (19 issues)

Critical: Remove CI/CD badges from README, standardize format count, update test metrics, complete CHANGELOG (Sessions 3-6), update CONTRIBUTING. Fix CLAUDE.md line 277 to say "(Docker)" or "(manual)" instead of "(CI)" for Snyk, CodeQL, and Gitleaks (CI/CD ban means these run via Docker/Makefile only).

High: Fix ARCHITECTURE.md platform status, deprecate root PLATFORM_STATUS.md, update CONTRIBUTORS/NEWS/SECURITY.

Medium: Fix API_CHANGELOG, rewrite CI_SETUP_GUIDE and DOWNLOAD_AND_INSTALL, delete 7 HTML duplicates.

### 9.4 New Documentation (10 files)

- `docs/USER_MANUAL_ANDROID.md`
- `docs/USER_MANUAL_DESKTOP.md`
- `docs/USER_MANUAL_WEB.md`
- `docs/SECURITY_EVENT_LOGGING.md` (Phase 2)
- `docs/SBOM_GUIDE.md` (Phase 2)
- `docs/GO_MODULES_REFERENCE.md` (Phase 5)
- `docs/TEST_COVERAGE_MATRIX.md` (Phase 4)
- `docs/CHALLENGE_BANKS_REFERENCE.md` (Phase 4)
- `docs/MONITORING_METRICS_REFERENCE.md` (Phase 3)
- `docs/NON_BLOCKING_ARCHITECTURE.md` (Phase 3)

### 9.5 Diagram Updates

Update 3 existing + create 4 new Mermaid diagrams:
- Update: architecture-overview, module-dependencies, security-scanning-pipeline, concurrency-safety
- New: performance-monitoring, go-module-integration, test-coverage-architecture, lazy-initialization-flow

### 9.6 KMP Module Documentation Refresh

All 10 modules: update README, add CHANGELOG 1.1.0 entry where modified, verify API reference and user guide accuracy.

### 9.7 Verification

- All markdown links valid (0 broken)
- All code references valid (0 stale)
- All metrics consistent across all docs

---

## 10. Phase 8: Content & Media

**Goal:** Update all 26 existing video scripts, add 5 new episodes (27-31), update all website content pages, create extended user guides.

### 10.1 Existing Video Scripts (26 updates)

All 26 existing scripts (episodes 1-26, where episode 26 is `ui-automation-testing`) updated with: accurate test counts (~14,150+ Kotlin, ~6,872 Go), format count (17 text + binary), module counts (10 KMP + 7 Go), platform status, security tool descriptions, monitoring infrastructure.

Major rewrites: `10-performance`, `12-testing`, `15-concurrency`.

### 10.2 New Video Episodes (5)

| # | Title | Track |
|---|-------|-------|
| 27 | Non-Blocking Architecture | Expert |
| 28 | Test Coverage Mastery | Expert |
| 29 | Performance Optimization | Expert |
| 30 | Autonomous QA | Expert |
| 31 | Project Completion Guide | Expert |

### 10.3 Website Updates (11 content pages + infrastructure)

All content pages in `website/app/` (root page + 10 page directories: about, architecture, changelog, cloud-storage, community, docs, download, faq, formats, video-course) plus `layout.tsx` infrastructure updated with current metrics, feature descriptions, platform status, architecture, changelog, video course listings.

### 10.4 Extended User Guides (7)

New: Quick Reference Card, Troubleshooting Guide, Format Migration Guide, Cloud Storage Setup, Developer Onboarding.
Extended: Deployment Guide, Build System.

### 10.5 Verification

- Metric consistency across all 31 scripts and 11 content pages
- External link verification
- Cross-reference accuracy
- Code example compilation check

---

## 11. Phase 9: Final Verification

**Goal:** Zero failures, zero disabled tests, zero broken documentation.

### 11.1 Full Test Suite

| Suite | Expected |
|-------|----------|
| `./gradlew :shared:desktopTest` | ~13,300+ pass, 0 fail (commonTest + desktopTest) |
| `./gradlew test koverHtmlReport` | All ~14,150+ Kotlin tests pass, ≥85% coverage |
| Go modules (7x `go test ./... -race -count=1`) | ~6,872 pass, 0 fail |
| `./gradlew runChallenges` | 33 banks pass |
| HelixQA pipeline | Full execution |
| `make qa-all` | Combined pass |
| `docker compose run --rm build ./docker/scripts/test-all.sh` | Full test suite passes inside container (CLAUDE.md mandate) |

### 11.2 Static Analysis & Security

| Tool | Expected |
|------|----------|
| Detekt | 0 issues |
| Gitleaks | 0 secrets |
| OWASP | 0 vulns above CVSS 7.0 |
| CycloneDX | SBOM generated |
| SonarQube | Quality gate passed |
| Snyk | 0 high/critical vulns |

### 11.3 Documentation Verification

- 0 broken markdown links
- 0 stale code references
- 0 metric inconsistencies
- 0 undocumented public APIs
- 100% SPDX header compliance

### 11.4 Performance Baselines

| Metric | Acceptance |
|--------|-----------|
| Parse time p95 | < 50ms |
| Cache hit ratio | > 80% |
| Format detection p95 | < 10ms |
| Heap growth under load | < 20% |
| Concurrent ops | 0 crashes, 0 deadlocks |
| Semaphore wait p95 | < 100ms |
| Startup (lazy init) | < 100ms |

### 11.5 Completion Report

`docs/COMPLETION_REPORT_2026-03-19-FINAL.md` with: executive summary, metrics dashboard, phase completion matrix, all fixes detailed, security scan results, performance baselines, test coverage matrix, known limitations, future recommendations.

### 11.6 Memory Update

Update project memory with final session metrics, test counts, file counts, challenge banks.

---

## 12. Success Criteria

The project is complete when ALL of the following are true:

- [ ] All ~14,150+ Kotlin tests pass with 0 failures (desktopTest: ~13,300+)
- [ ] All ~6,872 Go tests pass with 0 failures and `-race` flag
- [ ] All 33 challenge banks pass
- [ ] Detekt reports 0 issues
- [ ] Gitleaks reports 0 secrets
- [ ] OWASP reports 0 vulnerabilities above CVSS 7.0
- [ ] SBOM generated and valid
- [ ] SonarQube quality gate passed
- [ ] All 68+ source files have complete KDoc
- [ ] Dokka HTML API docs generated
- [ ] All markdown links valid
- [ ] All metrics consistent across all docs, video scripts, website
- [ ] 0 disabled/skipped tests anywhere
- [ ] 0 TODO/FIXME in production code without tracking reference
- [ ] Performance baselines within acceptance criteria
- [ ] 31 video course scripts accurate (26 existing + 5 new)
- [ ] 11 website content pages accurate
- [ ] Full test suite passes inside container
- [ ] Completion report written and committed
