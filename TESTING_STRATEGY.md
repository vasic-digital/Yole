# Yole Testing Strategy

**Version**: 2.0
**Date**: 2026-03-17

## Overview

Yole maintains a comprehensive test suite with **9,400+ test methods across ~215 test files**, covering 16 test types across 4 platform source sets (commonTest, desktopTest, androidUnitTest, wasmJsTest).

## Test Architecture

### Test Types (16 categories)

| Type | Count | Location |
|------|-------|----------|
| Unit | 3,000+ | `format/[name]/`, `network/`, `model/`, `util/` |
| Integration | 200+ | `format/integration/` |
| Stress | 300+ | `format/stress/`, `network/stress/` |
| Supremacy / Edge Case | 200+ | `format/supremacy/` |
| Mock HTTP | 150+ | `network/` |
| Property-Based | 100+ | Various |
| Contract | 100+ | `network/` |
| Security | 200+ | `network/`, `format/` |
| Performance | 100+ | `format/stress/PerformanceMetricsTests.kt` |
| Resilience | 150+ | `network/` |
| Fuzz | 100+ | `format/fuzz/` |
| Snapshot | 100+ | `format/snapshot/` |
| Load | 100+ | `format/load/` |
| E2E | 200+ | `format/e2e/` |
| Accessibility | 150+ | `ui/` |
| Non-Blocking | 100+ | `format/nonblocking/` |
| **Concurrency Safety** | 200+ | `util/ConcurrencySafetyTest.kt`, `concurrency/` |
| **OWASP Security** | 50+ | `network/security/` |
| **Monitoring / Metrics** | 50+ | `format/stress/MonitoringMetricsTests.kt` |

### Platform Test Matrix

| Source Set | Platform | Runner | Coroutine Support | MockK |
|------------|----------|--------|-------------------|-------|
| `commonTest` | All (JVM, Wasm, Native) | kotlin.test | `runBlocking<Unit>` only | No |
| `desktopTest` | JVM (Desktop) | JUnit4 | `runBlocking<Unit>` | Yes |
| `androidUnitTest` | JVM (Android) | JUnit4 | `runBlocking<Unit>` | Yes |
| `wasmJsTest` | Wasm/JS (Browser) | kotlin.test | `runBlocking<Unit>` only | No |

**Constraint**: `runTest` from `kotlinx-coroutines-test` is unavailable in `commonTest` because there is no Wasm variant. All tests use `runBlocking<Unit> { }` for JUnit4 compatibility.

## Test Organization

```
shared/src/commonTest/kotlin/digital/vasic/yole/
├── format/
│   ├── [18 format directories]/   # Per-format unit tests
│   ├── integration/                # Cross-format integration tests
│   ├── stress/                     # Stress and performance tests
│   │   ├── ComprehensiveStressTests.kt
│   │   ├── ConcurrentFormatParsingStressTest.kt
│   │   ├── EdgeCaseStressTest.kt
│   │   ├── FormatParsingStressTest.kt
│   │   ├── MonitoringMetricsTests.kt
│   │   └── PerformanceMetricsTests.kt
│   ├── supremacy/                  # Edge case and boundary tests
│   ├── fuzz/                       # Fuzz tests
│   ├── snapshot/                   # Snapshot tests
│   ├── load/                       # Load tests
│   ├── e2e/                        # End-to-end pipeline tests
│   └── nonblocking/                # Non-blocking verification tests
├── network/
│   ├── [protocol directories]/     # Protocol-specific tests
│   ├── stress/                     # Network stress tests
│   └── security/                   # OWASP and security tests
├── model/                          # Document model tests
├── ui/                             # UI and accessibility tests
├── util/                           # Utility tests (concurrency, rate limiting)
└── concurrency/                    # Concurrency safety tests
```

## Test Quality Gates

### Coverage Requirements
- All tests pass (100% success rate)
- No flaky tests allowed
- No tests may be removed, disabled, or skipped
- New code must not reduce overall coverage

### Performance Requirements
- Desktop test suite: < 5 minutes
- Individual parser test: < 30 seconds
- Stress tests: < 2 minutes each
- Full suite in container: < 15 minutes

## Concurrency Testing Strategy

All concurrency tests use KMP-compatible primitives:
- `Mutex` + `withLock` for shared state protection
- `Semaphore` for concurrency limiting
- `@Volatile` for lazy initialization caches
- `StateFlow.update{}` for atomic state emissions
- `synchronized(lock)` for registry operations

Lock ordering is enforced via code review and verified by `ConcurrencySafetyTest` and `ComprehensiveStressTests`. See `docs/LOCK_ORDERING.md` for the full hierarchy.

## Security Testing Strategy

### OWASP Top 10 Coverage
- Path traversal prevention (`normalizePath()` in all 8 protocol services)
- Input sanitization for all 18 format parsers
- Credential management via platform-specific `SecureStorage`
- Circuit breaker for denial-of-service protection

### Security Scanning Tools
- SonarQube (code quality, Docker)
- Snyk (dependency vulnerabilities, CI + Docker)
- CodeQL (semantic analysis, GitHub Actions)
- Gitleaks (secret detection, CI)
- Detekt (Kotlin static analysis, Gradle)
- OWASP Dependency Check (CVE database, Gradle)

## Monitoring and Metrics Testing

The `MonitoringMetricsTests` suite measures 6 categories with 42+ test methods:
1. Parse time monitoring (all 18 formats)
2. HTML generation monitoring (first call vs. cached)
3. Detection monitoring (extension vs. content)
4. Memory monitoring (allocation patterns)
5. Throughput monitoring (docs/second)
6. Lazy loading monitoring (parser instantiation timing)

## Running Tests

```bash
# Primary dev test command (no Android SDK needed)
./gradlew :shared:desktopTest

# Single test class
./gradlew :shared:desktopTest --tests "digital.vasic.yole.format.markdown.MarkdownParserTest"

# All stress tests
./gradlew :shared:desktopTest --tests "digital.vasic.yole.format.stress.*"

# All tests with coverage
./gradlew test koverHtmlReport

# In container (mandatory for CI)
docker compose run --rm build ./docker/scripts/test-all.sh
```

## Continuous Improvement

- Regular test reviews and updates
- Performance regression detection via CI baselines
- Coverage trend analysis via Kover reports
- New test types added as architecture evolves

---

**Document Version**: 2.0
**Last Updated**: 2026-03-17
