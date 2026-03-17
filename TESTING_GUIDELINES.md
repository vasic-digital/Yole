# Parser Testing Guidelines

**Version**: 2.0
**Date**: 2026-03-17
**Status**: Production-Ready
**Coverage**: 9,400+ tests across 215 test files

---

## Purpose

This document provides comprehensive guidelines for testing Yole parsers and the broader codebase, covering all 16 test types used in the project.

**Key Principle**: Targeted, coverage-driven testing achieves the best ROI. All 17 format parsers and 8 network protocol services are thoroughly tested.

---

## Test Coverage Summary

| Metric | Value |
|--------|-------|
| Total test methods | 9,400+ |
| Test files | ~215 |
| Source sets | commonTest, desktopTest, androidUnitTest, wasmJsTest |
| Format parsers tested | 17/17 |
| Protocol services tested | 8/8 |
| Test pass rate | 100% |

---

## 16 Test Types

### 1. Unit Tests
Test individual classes and functions in isolation.

**Location**: `shared/src/commonTest/kotlin/digital/vasic/yole/format/[format]/`
**Pattern**: One test class per parser, covering all parsing branches.

### 2. Integration Tests
Test cross-format interactions and module boundaries.

**Location**: `shared/src/commonTest/kotlin/digital/vasic/yole/format/integration/`
**Pattern**: Test FormatRegistry detection pipeline, format conversion, and parser interop.

### 3. Stress Tests
Verify behavior under high load and concurrent access.

**Location**: `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/`
**Suites**: `FormatParsingStressTest`, `ComprehensiveStressTests`, `ConcurrentFormatParsingStressTest`, `EdgeCaseStressTest`
**Pattern**: 100+ concurrent coroutines, 10,000+ line documents, sustained high-frequency operations.

### 4. Supremacy / Edge Case Tests
Test boundary conditions and unusual inputs.

**Location**: `shared/src/commonTest/kotlin/digital/vasic/yole/format/supremacy/`
**Pattern**: Empty content, whitespace-only, Unicode, mixed encodings, extremely long lines, deeply nested structures.

### 5. Mock HTTP Tests
Test network protocol services with mock HTTP responses.

**Location**: `shared/src/commonTest/kotlin/digital/vasic/yole/network/`
**Pattern**: Mock Ktor HTTP client with predefined responses for each protocol.

### 6. Property-Based Tests
Generate random inputs to find edge cases automatically.

**Pattern**: Random document generation, random format detection, random parse/serialize round-trips.

### 7. Contract Tests
Verify that all 8 protocol services implement `NetworkStorageService` consistently.

**Location**: `shared/src/commonTest/kotlin/digital/vasic/yole/network/`
**Pattern**: `ContractTestsForProtocols` verifies identical behavior across all services for connection, file operations, and error handling.

### 8. Security Tests
Test for path traversal, injection, and access control issues.

**Pattern**: Path traversal with `..`, `normalizePath()` validation, OWASP Top 10 patterns, input sanitization for all 17 parsers.

### 9. Performance Tests
Establish timing baselines and detect regressions.

**Location**: `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/PerformanceMetricsTests.kt`
**Baselines**: Parse time < 100ms for 10,000 lines, HTML cache hit < 1ms, format detection < 1ms.

### 10. Resilience Tests
Test CircuitBreaker, ConnectionLimiter, and recovery patterns.

**Location**: `shared/src/commonTest/kotlin/digital/vasic/yole/network/`
**Pattern**: Trip circuit breaker, saturate connection limiter, verify recovery after timeout.

### 11. Fuzz Tests
Feed semi-random and malformed inputs to parsers.

**Pattern**: Random byte sequences, truncated documents, mixed format syntax, invalid JSON for Jupyter.

### 12. Snapshot Tests
Compare parser output against known-good reference snapshots.

**Pattern**: Parse a fixed document, compare HTML output against saved snapshot string. Detect unintended changes.

### 13. Load Tests
Measure throughput and resource usage under sustained load.

**Location**: `shared/src/commonTest/kotlin/digital/vasic/yole/format/load/`
**Pattern**: Parse hundreds of documents sequentially and concurrently, measure throughput (docs/second).

### 14. E2E (End-to-End) Tests
Test the complete pipeline from raw input to rendered HTML.

**Location**: `shared/src/commonTest/kotlin/digital/vasic/yole/format/e2e/`
**Pattern**: Format detection -> parsing -> HTML generation -> CSS styling, for all 17 formats.

### 15. Accessibility Tests
Verify theme contrast ratios and WCAG compliance.

**Location**: `shared/src/commonTest/kotlin/digital/vasic/yole/ui/`
**Pattern**: Theme color contrast ratios, font size accessibility, screen reader compatibility metadata.

### 16. Non-Blocking Tests
Verify suspend functions do not block the calling thread.

**Location**: `shared/src/commonTest/kotlin/digital/vasic/yole/format/nonblocking/`
**Pattern**: Wrap operations in `withTimeout()` to detect blocking, verify `CancellationException` propagation.

---

## Testing Strategy

### 1. Identify High-Impact Targets

Use Kover coverage reports to find parsers with low branch coverage:

```bash
./gradlew :shared:koverXmlReport
```

Look for parsers with:
- Low branch coverage (<50%)
- High missed branch count (>30)
- Complex conditional logic
- Production usage

### 2. Understand the Implementation

Before writing tests, read the parser implementation to understand:
- Branch logic (if/when/try-catch)
- State management (lists, code blocks, tables)
- Format-specific features
- Edge cases

### 3. Write Tests by Category

Every parser test suite should include:
1. Format-specific feature tests
2. Metadata extraction tests (including defaults)
3. Validation tests (errors and valid input)
4. HTML generation tests (light/dark modes, escaping)
5. Edge case tests (empty, whitespace, Unicode)
6. Complex document test (all features combined)

---

## Concurrency Testing Guidelines

### Thread Safety Tests

All concurrency tests must use KMP-compatible patterns:

```kotlin
@Test
fun concurrentParsingProducesConsistentResults() = runBlocking<Unit> {
    val content = "# Test Document\nSome content"
    val results = (1..100).map {
        async(Dispatchers.Default) {
            MarkdownParser().parse(content)
        }
    }.awaitAll()

    // All results must be identical
    val reference = results.first().parsedContent
    results.forEach { assertEquals(reference, it.parsedContent) }
}
```

### Rules for Concurrency Tests

- Use `Mutex` and `Semaphore` from `kotlinx.coroutines.sync` (not `java.util.concurrent`)
- Use `Clock.System.now()` instead of `System.currentTimeMillis()`
- Use `delay()` instead of `Thread.sleep()`
- Use `runBlocking<Unit> { }` (not `runTest`) for JUnit4 compatibility
- Test lock ordering: always acquire locks in ascending priority order (see `LOCK_ORDERING.md`)
- Verify `CancellationException` is rethrown in all catch blocks
- Test `@Volatile` fields under concurrent access
- Test `StateFlow.update{}` atomic updates

### Key Concurrency Test Files

- `ConcurrencySafetyTest.kt` -- Mutex, channel, deadlock prevention
- `RaceConditionDetectionTest.kt` -- Check-then-act races
- `MemoryLeakDetectionTest.kt` -- FlowLazyLoader scope cancellation
- `StressAndIntegrationTest.kt` -- Rate limiter, lazy loading under 100+ concurrent requests

---

## Performance Baseline Guidelines

### Establishing Baselines

Performance tests use assertion-based thresholds to detect regressions:

```kotlin
@Test
fun markdownParsingUnder100ms() = runBlocking<Unit> {
    val content = generateMarkdownDocument(lines = 10000)
    val elapsed = measureTimeMillis {
        MarkdownParser().parse(content)
    }
    assertTrue(elapsed < 100, "Parsing took ${elapsed}ms, expected <100ms")
}
```

### Standard Baselines

| Operation | Threshold | Notes |
|-----------|-----------|-------|
| Parse 10,000 lines | < 100ms | Any format |
| First `toHtml()` call | < 50ms | Medium document |
| Cached `toHtml()` call | < 1ms | Returns cached HTML |
| `detectByExtension()` | < 1ms | O(1) map lookup |
| `detectByContent()` | < 5ms | Regex pattern matching |
| `registerAllParsersLazy()` | < 2ms | Factory lambdas only |
| FormatRegistry lazy init | < 5ms | First access to `formats` |
| StyleSheets cache hit | < 1ms | CSS from `styleSheetCache` |

### Running Performance Tests

```bash
# Desktop only (fast iteration)
./gradlew :shared:desktopTest \
  --tests "digital.vasic.yole.format.stress.PerformanceMetricsTests"

# All monitoring metrics
./gradlew :shared:desktopTest \
  --tests "digital.vasic.yole.format.stress.MonitoringMetricsTests"
```

---

## Test Structure Template

### File Organization

```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for [Parser Name]
 *
 *########################################################*/
package digital.vasic.yole.format.[formatname]

import digital.vasic.yole.format.TextParser
import kotlin.test.*

class [Parser]ComprehensiveTest {

    private lateinit var parser: [Parser]

    @BeforeTest
    fun setup() {
        parser = [Parser]()
    }

    // ==================== Feature Tests ====================

    @Test
    fun `should parse [feature] correctly`() {
        val content = "[format-specific syntax]"
        val doc = parser.parse(content)
        assertTrue(doc.parsedContent.contains("[expected]"))
    }

    // ==================== Metadata Tests ====================
    // ==================== Validation Tests ====================
    // ==================== HTML Generation Tests ====================
    // ==================== Edge Cases ====================
    // ==================== Complex Document Tests ====================
}
```

---

## Test Constraints

- **JUnit4 runner**: Tests use `runBlocking<Unit> { }` (not `runTest`). JUnit4 requires `Unit` return type; `runTest` returns `TestResult` which causes `void` signature mismatch.
- **MockK is JVM-only**: Available in `desktopTest` and `androidUnitTest`, NOT in `commonTest` or `wasmJsTest`.
- **kotlinx-coroutines-test**: No WASM variant. Unavailable in `commonTest`.
- **jvmTarget**: Must be `"11"` in all JVM compilations.

---

## Test Naming Conventions

### Good Test Names
- `should convert level 1 heading to HTML`
- `should detect unclosed code block`
- `should handle source as array instead of string`
- `should escape HTML special characters`
- `concurrentParsingShouldNotCorruptResults`

### Rules
1. Use backticks for descriptive names
2. Start with "should" for behavior tests
3. Be specific about what is being tested
4. Include the expected behavior

---

## Coverage Measurement

### Generate Coverage Report

```bash
./gradlew test koverHtmlReport
# Report at: shared/build/reports/kover/html/index.html
```

### Target Coverage

- **Parser-specific**: 80-90% branch coverage
- **Project overall**: 63%+ line coverage (current baseline)
- **New code**: Must not reduce overall coverage

---

## Troubleshooting

### String interpolation in test content
```kotlin
// WRONG - $ will be interpreted
val content = "$x = y$"

// CORRECT - Escape the $
val content = "\$x = y\$"
```

### Assertion expects exact HTML structure
```kotlin
// FRAGILE - Expects exact tag structure
assertTrue(html.contains("<ul><li>Item</li></ul>"))

// ROBUST - Check for key elements
assertTrue(html.contains("<ul>"))
assertTrue(html.contains("<li>Item</li>"))
```

### Test using wrong coroutine runner
```kotlin
// WRONG - runTest returns TestResult, incompatible with JUnit4
@Test
fun myTest() = runTest { ... }

// CORRECT - runBlocking<Unit> returns Unit
@Test
fun myTest() = runBlocking<Unit> { ... }
```

---

## Quality Standards

### Minimum Requirements
- 100% test pass rate before committing
- All major features covered (at least one test per feature)
- Edge cases included (empty, unicode, malformed)
- Clear test names (descriptive, follows naming convention)
- No tests may ever be removed, disabled, or skipped

### Aspirational Goals
- 80%+ branch coverage for each parser
- 90%+ line coverage for each parser
- Complex document test exercising all features
- Performance considerations (tests run in <1s total)

---

**Document Version**: 2.0
**Last Updated**: 2026-03-17
**Maintained By**: Engineering Team
