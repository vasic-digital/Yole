<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Module 25: Complete Test Coverage (8 videos)

## Video 25.1: The 9,400+ Test Suite Architecture (18 min)

### Timestamps
- 0:00 Introduction: Yole's testing philosophy -- never remove, disable, or skip tests
- 2:00 Test count breakdown: 9,400+ methods across ~215 files, 4 source sets
- 4:00 The 16 test types: unit, integration, stress, supremacy, mock HTTP, property-based, contract, security, performance, resilience, fuzz, snapshot, load, E2E, accessibility, non-blocking
- 6:00 Test directory structure: format/, network/, model/, ui/, util/, concurrency/
- 8:00 Platform source sets: commonTest (all), desktopTest (JVM + MockK), androidUnitTest, wasmJsTest
- 10:00 The JUnit4 constraint: runBlocking<Unit> everywhere
- 12:00 MockK availability: JVM-only (desktopTest + androidUnitTest)
- 14:00 Coverage measurement: Kover with 63%+ line coverage, 50% minimum gate
- 16:00 Test execution: `./gradlew :shared:desktopTest` for fast iteration
- 17:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/` -- All test directories
- `TESTING_GUIDELINES.md` -- Testing conventions
- `TESTING_STRATEGY.md` -- Overall strategy

---

## Video 25.2: Unit and Integration Testing Patterns (18 min)

### Timestamps
- 0:00 Unit test structure: one test class per parser/service
- 2:00 The test template: BeforeTest setup, AfterTest teardown, categorized tests
- 4:00 Format-specific feature tests: parsing each format's unique syntax
- 6:00 Metadata extraction tests: including defaults for missing fields
- 8:00 Validation tests: detecting errors and accepting valid input
- 10:00 HTML generation tests: light/dark modes, escaping, structure
- 12:00 Integration tests: cross-format detection pipeline, FormatRegistry
- 14:00 Contract tests: all 8 protocol services implementing NetworkStorageService identically
- 16:00 Writing effective assertions: robust vs. fragile HTML checks
- 17:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/jupyter/JupyterParserComprehensiveTest.kt` -- Gold standard
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/integration/`

---

## Video 25.3: Fuzz, Snapshot, and Property-Based Testing (18 min)

### Timestamps
- 0:00 Fuzz testing: feeding semi-random inputs to find crashes
- 2:00 Yole's fuzz tests: random byte sequences, truncated documents, mixed syntax
- 4:00 The key property: parsers must never throw on any input (return errors instead)
- 6:00 Snapshot testing: comparing parser output against reference strings
- 8:00 Creating and updating snapshots: when format output changes intentionally
- 10:00 Snapshot test for all 17 formats: detecting unintended output changes
- 12:00 Property-based testing: random document generation with format-specific generators
- 14:00 Round-trip properties: parse -> toHtml -> verify structure preserved
- 16:00 Combining fuzz + snapshot: fuzz discovers inputs, snapshot locks correct behavior
- 17:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/fuzz/`
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/snapshot/`

---

## Video 25.4: Stress and Load Testing (20 min)

### Timestamps
- 0:00 Stress testing: pushing the system to its limits
- 2:00 FormatParsingStressTest: all 17 formats with large inputs
- 4:00 ComprehensiveStressTests: 30 tests for concurrent parsing and cache contention
- 6:00 ConcurrentFormatParsingStressTest: 100 coroutines parsing simultaneously
- 8:00 EdgeCaseStressTest: boundary conditions with concurrent access
- 10:00 Load testing: measuring throughput (documents/second)
- 12:00 FormatLoadTests: sequential and concurrent parsing throughput
- 14:00 Network stress tests: protocol overload, circuit breaker under load
- 16:00 Database stress tests: concurrent metadata reads/writes
- 18:00 Writing reproducible stress tests: fixed seeds, bounded inputs
- 19:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/`
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/load/`
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/stress/`

---

## Video 25.5: E2E and Non-Blocking Testing (18 min)

### Timestamps
- 0:00 E2E testing: full pipeline from raw input to rendered HTML
- 2:00 EndToEndFormatTests: detection -> parsing -> HTML -> CSS for all 17 formats
- 4:00 Measuring p50/p95/p99 latencies across the pipeline
- 6:00 Large document E2E: 1,000-line and 10,000-line benchmarks
- 8:00 Non-blocking verification: the withTimeout pattern
- 10:00 Testing that suspend functions actually suspend (yield interleaving)
- 12:00 CancellationException propagation tests
- 14:00 Verifying non-blocking across all 8 protocol services
- 16:00 serviceScope lifecycle: cancellation on reconnect/disconnect
- 17:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/e2e/EndToEndFormatTests.kt`
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/nonblocking/NonBlockingOperationTests.kt`

---

## Video 25.6: Security and Resilience Testing (18 min)

### Timestamps
- 0:00 Security testing: OWASP patterns applied to Yole
- 2:00 Path traversal tests: normalizePath() validation in all 8 protocols
- 4:00 Input sanitization: testing all 17 parsers with injection payloads
- 6:00 Resilience testing: CircuitBreaker state transitions
- 8:00 ConnectionLimiter saturation: verifying excess requests suspend
- 10:00 Rate limiter testing: TokenBucket exhaustion, adaptive rate adjustment
- 12:00 Recovery testing: verifying service recovery after transient failures
- 14:00 Mock HTTP testing: predefined responses for protocol verification
- 16:00 Contract tests: consistent behavior across all 8 services
- 17:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/`

---

## Video 25.7: Accessibility and Platform-Specific Testing (15 min)

### Timestamps
- 0:00 Accessibility testing: theme contrast ratios, WCAG compliance
- 2:00 ThemeAccessibilityTests: verifying color contrast for all themes
- 4:00 Font size accessibility: minimum sizes, scaling behavior
- 6:00 Platform-specific tests: desktop (MockK), Wasm (browser constraints)
- 8:00 desktopTest source set: leveraging MockK for mocking
- 10:00 wasmJsTest source set: testing within browser sandbox limits
- 12:00 Android-specific tests: using androidUnitTest source set
- 14:00 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/ui/`
- `shared/src/desktopTest/`
- `shared/src/wasmJsTest/`

---

## Video 25.8: Test Coverage Strategy and CI Integration (15 min)

### Timestamps
- 0:00 Kover coverage: 63%+ line coverage with 50% minimum gate
- 2:00 Identifying coverage gaps: using koverXmlReport analysis
- 4:00 Targeted testing: coverage-driven test writing for maximum ROI
- 6:00 CI integration: all 9,400+ tests run on every push and PR
- 8:00 Coverage trend tracking: Codecov integration
- 10:00 The challenge framework: Go-based cross-platform validation
- 12:00 Maintaining the test suite: the golden rule (never remove or disable)
- 14:00 Summary

### Exercises
1. **Add tests for a low-coverage parser** -- Run Kover, find the parser with lowest branch coverage, and add 20+ tests to improve it by at least 20%.
2. **Write a fuzz test for a new format** -- Create a fuzz test that generates random inputs for a parser and verifies it never throws.
3. **Create an E2E test** -- Write a test that exercises the complete pipeline for all 17 formats and measures p99 latency.
4. **Set up coverage gating** -- Configure Kover to fail the build if coverage drops below 60%.
