# Module 11: Testing Strategies (8 videos)

## Video 11.1: Comprehensive Testing Strategy (22 min)

### Timestamps
- 0:00 Introduction: why Yole maintains 9,400+ test methods across 195 test files
- 1:30 The golden rule: **never remove, disable, skip, or leave broken tests**
- 3:00 Test pyramid: unit tests (parsers), integration tests (cross-format), end-to-end tests (full pipeline)
- 5:00 KMP testing architecture: `commonTest` for shared logic, `platformTest` for platform-specific behavior
- 7:00 Test source set structure: `shared/src/commonTest/kotlin/digital/vasic/yole/`
- 9:00 Test categories in Yole: format tests, integration tests, stress tests, supremacy (edge case) tests, resilience tests (CircuitBreaker, ConnectionLimiter, DocumentCache), contract tests (8 protocols), safety tests, security validation tests, monitoring metrics tests, performance metrics tests
- 11:00 Coverage targets and Kover configuration in `shared/build.gradle.kts`
- 13:00 Running the full test suite: `./gradlew test`
- 14:00 Running a single test class: `./gradlew test --tests "digital.vasic.yole.format.todotxt.TodoTxtParserTest"`
- 15:00 Running with coverage: `./gradlew test koverHtmlReport`
- 16:00 Test naming conventions: classes end with `Test` or `Tests`
- 17:00 Mandatory Docker container testing: `docker compose run --rm build ./docker/scripts/test-all.sh`
- 18:30 The "never disable tests" rule in depth: fix root causes, not symptoms
- 20:00 When tests fail: fix source to match tests (if tests are correct) or fix tests to match source (if source is correct)
- 21:00 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/` -- All format parser tests (one directory per format)
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/integration/CrossFormatIntegrationTest.kt` -- Cross-format integration tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/FormatParsingStressTest.kt` -- Parsing stress tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/EdgeCaseStressTest.kt` -- Edge case stress tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/supremacy/UltimateSupremacyTest.kt` -- Boundary/edge case tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/FormatRegistryStressTest.kt` -- Format registry stress tests
- `shared/build.gradle.kts` -- Kover configuration for coverage reporting
- `docker/scripts/test-all.sh` -- Comprehensive multi-platform test runner

### The "Never Disable Tests" Rule (from CLAUDE.md)

This is a mandatory project policy:

```
**NO test may ever be removed, disabled, skipped, or left broken!**

All issues must be fixed by addressing the root causes:
- Fix the source code to match tests if tests are correct
- Fix the tests to match source code if source is correct
- Add missing classes/methods to make code compile
- Add missing imports to tests
- Fix syntax errors
- Fix parameter name mismatches
```

### Key Test Statistics
- **9,400+ test methods** across **195 test files** in commonTest + desktopTest + androidUnitTest + wasmJsTest
- Test categories: format parsers, model, network, UI, security, concurrency, stress, integration, property-based, contract, resilience, monitoring, performance
- Dedicated test suites: SafetyFixesTest (92 tests), ContractTestsForProtocols (89 tests), ResilienceTests (53 tests), MonitoringMetricsTests (42 tests), SecurityValidationTests (36 tests), ComprehensiveStressTests (30 tests), PerformanceMetricsTests (29 tests)
- All tests run on every commit via CI/CD
- 63.0% line coverage (measured by Kover)

### Exercises
1. **Write a format parser test** -- Pick any format (e.g., CSV or LaTeX) and write a new test case that verifies parsing of a specific edge case. Add it to the appropriate test file.
2. **Run the full test suite** -- Execute `./gradlew test` and review the output. Identify the total number of tests and any that take longer than 1 second.
3. **Generate a coverage report** -- Run `./gradlew test koverHtmlReport`, open the HTML report, and identify which format parsers have the highest and lowest coverage.

---

## Video 11.2: Property-Based Testing (18 min)

### Timestamps
- 0:00 What is property-based testing and why it matters
- 2:00 The invariant: `parse(format(x)) == x` (roundtrip property)
- 4:00 Generating random inputs for parser testing
- 6:00 Configuring Kotest property-based testing in Yole
- 8:00 Writing generators for TodoTxt tasks
- 10:00 Writing generators for Markdown documents
- 12:00 Edge case discovery through fuzzing: how random inputs find bugs
- 14:00 Shrinking: when a test fails, find the minimal failing input
- 16:00 Integration with existing test suites
- 17:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/todotxt/TodoTxtParserTest.kt` -- Property-based tests for Todo.txt parsing
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/markdown/MarkdownParserTest.kt` -- Markdown parser tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/FormatParsingStressTest.kt` -- Stress tests that exercise parsers with varied inputs

### Key Concept: Roundtrip Testing

```kotlin
// Property: parsing a formatted task should produce the original task
forAll(todoTxtTaskGenerator) { task ->
    val formatted = task.toTodoTxtLine()
    val parsed = TodoTxtParser.parseLine(formatted)
    parsed.priority shouldBe task.priority
    parsed.description shouldBe task.description
    parsed.projects shouldBe task.projects
    parsed.contexts shouldBe task.contexts
}
```

### Exercises
1. **Write a property test for CSV** -- Create a generator for CSV rows and verify that parsing and re-serializing produces equivalent output.
2. **Test with Unicode** -- Write a property test that generates random Unicode strings and verifies parsers handle them without crashing.
3. **Find a boundary** -- Use property-based testing to find the maximum input size that a parser handles within 100ms.

---

## Video 11.3: UI Testing Across Platforms (15 min)

### Timestamps
- 0:00 Compose Multiplatform testing overview
- 2:00 Compose test rules and ComposeTestRule setup
- 4:00 Semantic matchers: finding elements by text, role, content description
- 6:00 Interaction testing: clicks, swipes, text input
- 8:00 Screenshot testing for visual regression
- 10:00 Platform-specific test utilities: Android Espresso, Desktop robot
- 12:00 Accessibility testing integration
- 14:00 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/ui/ThemeTest.kt` -- Theme unit tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/ui/AccessibilityTest.kt` -- Accessibility tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/ui/AnimationTests.kt` -- Animation tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/ui/AnimationConstantsTest.kt` -- Animation constants tests

### Exercises
1. **Write a theme test** -- Create a test that verifies dark mode colors are different from light mode colors for at least 5 theme tokens.
2. **Test accessibility labels** -- Write a test that verifies all interactive UI elements have proper content descriptions for screen readers.
3. **Visual regression** -- Take screenshots of the editor in light and dark mode, then modify a theme color and verify the screenshot test detects the change.

---

## Videos 11.4-11.8: Advanced Testing

### Video 11.4: Concurrency Testing (15 min)

#### Timestamps
- 0:00 Concurrency bugs in multi-platform applications
- 2:00 Race conditions in parser initialization
- 4:00 Testing with `kotlinx.coroutines.test`: `runTest`, `TestDispatcher`
- 6:00 Deadlock detection patterns
- 8:00 Concurrent document access testing
- 10:00 Thread-safe lazy initialization tests
- 12:00 Stress testing with parallel coroutines
- 14:00 Summary

#### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/concurrency/ConcurrencySafetyTest.kt` -- Concurrency safety tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/util/ConcurrencySafetyTest.kt` -- Utility concurrency tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/util/StressAndIntegrationTest.kt` -- Stress and integration tests

#### Exercises
1. **Write a race condition test** -- Create a test where 100 coroutines simultaneously access the `FormatRegistry` and verify no data corruption occurs.

---

### Video 11.5: Performance Testing and Benchmarking (15 min)

#### Timestamps
- 0:00 Performance testing vs. benchmarking
- 2:00 Setting up the benchmark source set (`commonBenchmark`, `desktopBenchmark`)
- 4:00 Writing parser benchmarks: measure parse time for large documents
- 6:00 Memory benchmarks: tracking allocations during parsing
- 8:00 Regression detection: comparing against baseline
- 10:00 Benchmark CI integration
- 12:00 Real-world performance scenarios
- 14:00 Summary

#### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/FormatParsingStressTest.kt` -- Parsing performance stress tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/NetworkPerformanceTest.kt` -- Network performance tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/ResourceManagementStressTest.kt` -- Resource management stress tests

#### Exercises
1. **Benchmark a parser** -- Time how long `MarkdownParser` takes to parse a 10,000-line Markdown file. Compare with `PlainTextParser` on the same size input.

---

### Video 11.6: Security Testing (15 min)

#### Timestamps
- 0:00 Security testing in a text editor context
- 2:00 Input validation: preventing injection through crafted file content
- 4:00 HTML output sanitization: XSS prevention in preview
- 6:00 Path traversal prevention in file operations
- 8:00 Network credential security: testing token storage
- 10:00 Fuzz testing for security vulnerabilities
- 12:00 Dependency vulnerability scanning
- 14:00 Summary

#### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/security/InputValidationSecurityTest.kt` -- Input validation security tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/safety/NullSafetyTest.kt` -- Null safety tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/platform/SecureStorageErrorHandlingTest.kt` -- Secure storage error handling tests

#### Exercises
1. **Test XSS prevention** -- Write a test that includes `<script>alert('xss')</script>` in a Markdown document and verify the HTML output is properly sanitized.

---

### Video 11.7: Mock Strategies for Network and File I/O (12 min)

#### Timestamps
- 0:00 Why mock in KMP: platform boundaries
- 2:00 The `MockNetworkStorageService` implementation
- 4:00 Mocking Ktor HttpClient responses
- 6:00 Mocking file system operations with Okio
- 8:00 MockK library usage patterns in Yole
- 10:00 When to mock vs. when to use integration tests
- 11:30 Summary

#### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocol/MockNetworkStorageService.kt` -- Mock implementation of NetworkStorageService
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/protocol/MockNetworkStorageServiceTest.kt` -- Mock service tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/NetworkStorageIntegrationTest.kt` -- Integration tests using mocks
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/NetworkIntegrationComprehensiveTest.kt` -- Comprehensive integration tests

#### Exercises
1. **Create a mock scenario** -- Using `MockNetworkStorageService`, write a test that simulates uploading a file, listing files to verify it exists, and then deleting it.

---

### Video 11.8: Continuous Testing and Test Infrastructure (15 min)

#### Timestamps
- 0:00 CI/CD test integration overview
- 2:00 GitHub Actions workflow for KMP tests
- 4:00 Matrix builds: testing across JVM, Wasm, and Native targets
- 6:00 Test parallelization and sharding
- 8:00 Flaky test detection and mitigation
- 10:00 Test reporting and notifications
- 12:00 Docker-based test execution: `docker compose run --rm build ./docker/scripts/test-all.sh`
- 14:00 Summary

#### Code References
- `docker/scripts/test-all.sh` -- Multi-platform test runner script
- `shared/build.gradle.kts` -- Test configuration and Kover setup
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/FormatRegistryStressTest.kt` -- Registry stress tests (example of tests that detect regressions)

#### Exercises
1. **Add edge case tests** -- Choose any format parser and add 3 new edge case tests: empty input, maximum length input, and input with only special characters.
2. **Run tests in Docker** -- Execute `docker compose run --rm build ./docker/scripts/test-all.sh` and compare the results with running `./gradlew test` directly. Note any differences.
3. **Coverage gap analysis** -- Generate a Kover report and identify the top 3 classes with the lowest test coverage. Write at least one new test for each.
