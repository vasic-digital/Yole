<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Module 28: Test Coverage Mastery (8 videos)

## Learning Objectives

- Understand all 16 test types used in Yole and when to apply each
- Learn the architectural constraints that shape test design in KMP (JUnit4, no runTest, MockK JVM-only)
- Master fuzz, snapshot, and property-based testing for parser validation
- Build confidence in resilience, security, and accessibility testing patterns
- Design a test strategy that scales to 10,000+ tests without becoming brittle

---

## Video 28.1: The 16 Test Types Overview (18 min)

### Timestamps
- 0:00 Introduction: why 16 test types and how they complement each other
- 2:00 The test pyramid for a cross-platform text editor: unit at the base, E2E at the top
- 4:00 Type 1 -- Unit tests: isolated function and class testing, the bulk of the 10,000+ suite
- 5:00 Type 2 -- Integration tests: cross-format interactions, registry + parser + cache pipelines
- 6:00 Type 3 -- Stress tests: concurrent parsing, cache overflow, rate limiter saturation
- 7:00 Type 4 -- Fuzz tests: random input generation for parser robustness
- 8:00 Type 5 -- Snapshot tests: golden-file comparison for HTML output stability
- 9:00 Type 6 -- Load tests: high-volume sequential operations measuring throughput
- 10:00 Type 7 -- E2E tests: full pipeline from raw text to rendered HTML with assertions
- 11:00 Type 8 -- Performance tests: baseline latency measurements with regression thresholds
- 12:00 Type 9 -- Accessibility tests: WCAG compliance in generated HTML output
- 13:00 Type 10 -- Security tests: path traversal, injection, malicious input validation
- 14:00 Type 11 -- Resilience tests: circuit breaker state transitions, connection recovery
- 15:00 Type 12 -- Property-based tests: invariant verification across random inputs
- 16:00 Type 13 -- Contract tests: API stability between modules and facade bridges
- 16:30 Type 14 -- Non-blocking tests: verifying no thread stalls during operations
- 17:00 Type 15 -- Supremacy tests: edge cases and boundary conditions
- 17:30 Type 16 -- Mock HTTP tests: protocol service testing with simulated server responses
- 18:00 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/` -- All test type directories
- `shared/src/desktopTest/kotlin/digital/vasic/yole/` -- JVM-specific tests with MockK
- `shared/src/wasmJsTest/kotlin/digital/vasic/yole/` -- Wasm-specific tests

---

## Video 28.2: Unit and Integration Testing (16 min)

### Timestamps
- 0:00 Unit test anatomy: arrange, act, assert with runBlocking<Unit>
- 2:00 Why runBlocking<Unit> instead of runTest: JUnit4 requires void return, runTest returns TestResult
- 4:00 Testing parsers: input text to ParsedDocument, asserting parsed content and metadata
- 6:00 Testing FormatRegistry: format detection by extension and by content pattern matching
- 8:00 Integration tests: full pipeline from raw Markdown through parsing to HTML generation
- 10:00 Cross-format integration: opening a file detected as one format, re-detecting as another
- 12:00 DocumentCache integration: parse, cache, retrieve, verify cache hit/miss counters
- 14:00 Network integration tests: OAuth2Flow through AuthTokenManager to protocol service
- 15:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/markdown/` -- Markdown unit tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/integration/` -- Integration tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/todotxt/` -- Todo.txt tests

---

## Video 28.3: Stress and Load Testing (16 min)

### Timestamps
- 0:00 Stress test philosophy: push the system beyond normal operating limits
- 2:00 Concurrent parsing stress: 50 coroutines parsing different formats simultaneously
- 4:00 Cache overflow stress: exceeding LRU capacity and verifying eviction correctness
- 6:00 Rate limiter saturation: 100 concurrent requests against a 10-permit semaphore
- 8:00 Circuit breaker stress: rapid failure/recovery cycles testing state machine integrity
- 10:00 Load testing: 1000 sequential operations measuring throughput degradation
- 12:00 Memory pressure: parsing progressively larger documents, verifying no OOM
- 14:00 Responsiveness guarantee: all operations complete within p99 latency bounds
- 15:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/` -- Stress test directory
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/` -- Network stress tests

---

## Video 28.4: Fuzz, Snapshot, and Property-Based Testing (18 min)

### Timestamps
- 0:00 Fuzz testing: generating random, malformed, and adversarial inputs
- 2:00 Character-level fuzzing: random Unicode, control characters, null bytes in parser input
- 4:00 Structure-level fuzzing: malformed Markdown headers, unclosed HTML tags, invalid YAML
- 6:00 Why parsers must never throw: every fuzz input must produce a ParsedDocument (possibly with errors list)
- 8:00 Snapshot testing: golden-file HTML output comparison for regression detection
- 10:00 Snapshot update workflow: regenerating golden files when intentional changes are made
- 12:00 Property-based testing: defining invariants that hold for all inputs
- 14:00 Property example: "parsing then serializing a Todo.txt document preserves all tasks"
- 16:00 Property example: "format detection is idempotent -- detecting twice gives the same result"
- 17:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/` -- Fuzz test files
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/` -- Snapshot test files

---

## Video 28.5: E2E and Non-Blocking Testing (16 min)

### Timestamps
- 0:00 E2E testing scope: user intent to visible output, crossing all architectural layers
- 2:00 E2E example: "user opens a Markdown file" -- file read, detect format, parse, generate HTML, apply CSS
- 4:00 E2E with network: connecting to a protocol, listing files, downloading, parsing
- 6:00 Non-blocking testing: verifying operations do not block the calling thread
- 8:00 Pattern: withTimeout + async to detect accidental blocking
- 10:00 Main-thread safety: testing that UI-triggering operations dispatch correctly
- 12:00 Cancellation E2E: starting a long operation, cancelling, and verifying clean state
- 14:00 Combining E2E with performance: asserting the full pipeline completes within latency bounds
- 15:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/` -- E2E test files
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/` -- Non-blocking tests

---

## Video 28.6: Security and Resilience Testing (16 min)

### Timestamps
- 0:00 Security testing in a text editor: what attackers target
- 2:00 Path traversal tests: ../../etc/passwd in file paths, normalizePath() defense
- 4:00 Injection tests: XSS payloads in Markdown, script injection in HTML output
- 6:00 Malicious format tests: ZIP bombs disguised as documents, oversized inputs
- 8:00 Resilience testing: simulating network failures, timeout scenarios
- 10:00 Circuit breaker tests: verifying Open state after N consecutive failures
- 12:00 Connection recovery tests: reconnecting after circuit breaker reset
- 14:00 Offline queue tests: operations queued during outage, drained on reconnect
- 15:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/` -- Security and resilience tests
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/PathUtils.kt` -- Path normalization
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/CircuitBreaker.kt` -- Circuit breaker

---

## Video 28.7: Accessibility, Contract, and Mock HTTP Testing (16 min)

### Timestamps
- 0:00 Accessibility testing: why generated HTML must meet WCAG standards
- 2:00 Color contrast tests: verifying theme colors meet 4.5:1 minimum ratio
- 4:00 Semantic HTML tests: headings hierarchy, alt text, ARIA labels in generated output
- 6:00 Theme accessibility: testing both light and dark theme CSS generation
- 8:00 Contract testing: API boundaries between shared module and platform modules
- 10:00 Facade bridge contracts: typealiases must re-export the correct types
- 12:00 Mock HTTP testing: simulating server responses for all 8 protocol services
- 14:00 Mock scenarios: success, error, timeout, malformed JSON, rate limiting
- 15:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/` -- Accessibility tests
- `shared/src/desktopTest/kotlin/digital/vasic/yole/` -- MockK-based tests

---

## Video 28.8: Scaling to 10,000+ Tests (14 min)

### Timestamps
- 0:00 Test organization: directory structure mirroring source code
- 2:00 Test naming conventions: descriptive names that serve as documentation
- 4:00 Test isolation: each test creates its own state, no shared mutable fixtures
- 6:00 Test speed: keeping the full suite under 5 minutes on host JVM
- 8:00 Platform-specific test sets: commonTest for all, desktopTest for MockK, wasmJsTest for browser
- 10:00 Coverage reporting: Kover HTML reports and tracking coverage trends
- 12:00 The rule: never remove, disable, or skip a test -- fix root causes instead
- 13:30 Summary

### Exercises
1. **Add a fuzz test**: Write a fuzz test for the CSV parser that generates 100 random CSV inputs (varying delimiters, quoting, line endings) and asserts no exceptions are thrown.
2. **Add a snapshot test**: Create a golden-file snapshot test for LaTeX parsing that compares the generated HTML against a stored reference file.
3. **Property-based invariant**: Define and test the property: "For any valid Markdown input, parsing twice produces identical ParsedDocument instances."
4. **Security test**: Write a test that attempts path traversal through the SFTP service and verifies normalizePath() blocks the attack.
5. **Accessibility audit**: Write a test that checks all 17 format CSS stylesheets for color contrast ratios above WCAG AA minimum.
