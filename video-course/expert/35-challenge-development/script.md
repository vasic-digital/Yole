<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Module 35: Challenge-Driven Development (8 videos)

## Prerequisites

- Module 16: Challenges Framework
- Module 11: Testing Strategies
- Module 25: Complete Test Coverage

## Learning Objectives

- Design challenge banks that encode expected application behaviour as executable specifications
- Write challenge JSON/YAML schemas that target specific platforms and feature areas
- Integrate challenge validation into the Gradle build via the runChallenges task
- Use challenge results to drive bug fixes and test coverage improvements
- Extend the challenge framework with new banks for new feature areas

---

## Video 35.1: What Is Challenge-Driven Development? (12 min)

### Timestamps
- 0:00 Introduction: challenges as executable specifications for application behaviour
- 2:00 The distinction: challenges test the app as a whole, unit tests test components in isolation
- 4:00 The Challenges Go submodule: structure, packages, and CLI interface
- 6:00 Challenge categories in Yole: security, format-edge-cases, protocol-resilience, UI automation
- 8:00 The feedback loop: challenge fails -> investigation -> fix -> challenge passes -> test added
- 10:00 Why challenges complement unit tests rather than replace them
- 11:30 Summary

### Code References
- `Challenges/` -- Go submodule root
- `Makefile` -- `make challenge` and `make qa-all` targets
- `shared/build.gradle.kts` -- runChallenges Gradle task

---

## Video 35.2: Challenge Bank Schema (14 min)

### Timestamps
- 0:00 Bank schema overview: id, name, platform, priority, scenarios
- 2:00 Scenario fields: id, description, steps, expected outcomes, documentation references
- 4:00 Platform targeting: android, desktop, web, all
- 6:00 Priority levels: critical, high, medium, low -- and how they affect run order
- 8:00 Documentation refs: linking each scenario to the relevant spec or user story
- 10:00 Challenges bridge: how the bank schema maps to HelixQA test bank entries
- 12:00 Versioning banks: schema version field and backward compatibility
- 13:30 Summary

### Code References
- `Challenges/banks/` -- Existing challenge bank YAML/JSON files
- `Challenges/pkg/` -- Go package implementing bank loading and execution

---

## Video 35.3: The Three Existing Challenge Banks (14 min)

### Timestamps
- 0:00 Bank 1: security challenges -- path traversal, credential exposure, token refresh
- 2:00 Security challenge walkthrough: verify normalizePath() blocks ../../../etc/passwd
- 4:00 Bank 2: format-edge-cases -- malformed input, empty files, unicode, maximum size
- 6:00 Format edge-case walkthrough: parsing a 100MB Markdown file without OOM
- 8:00 Bank 3: protocol-resilience -- timeout, server disconnect, partial upload, reconnect
- 10:00 Resilience challenge walkthrough: FTP server drops connection mid-transfer
- 12:00 How findings from these banks drove real bug fixes in Sessions 3 and 7
- 13:30 Summary

### Code References
- `Challenges/banks/security.yml`
- `Challenges/banks/format-edge-cases.yml`
- `Challenges/banks/protocol-resilience.yml`

---

## Video 35.4: Writing a New Challenge Bank (16 min)

### Timestamps
- 0:00 Choosing a scope: what feature area is not yet covered by an existing bank?
- 2:00 Case study: writing a new concurrency-safety bank for Session 7 fixes
- 4:00 Scenario 1: concurrent connect and cancel -- verify no resource leak
- 6:00 Scenario 2: rapid format switching -- verify FormatRegistry remains consistent
- 8:00 Scenario 3: simultaneous cloud sync and local edit -- verify no data corruption
- 10:00 Scenario 4: circuit breaker recovery -- verify service accepts new requests after cooldown
- 12:00 Validating the bank schema: running the bank loader in dry-run mode
- 14:00 Submitting the bank: PR process and review checklist
- 15:30 Summary

### Code References
- `Challenges/banks/` -- Bank directory
- `Challenges/pkg/runner/` -- Go runner package

---

## Video 35.5: The runChallenges Gradle Task (12 min)

### Timestamps
- 0:00 Task registration in shared/build.gradle.kts: exec { } invoking the Go binary
- 2:00 Task inputs: bank path, platform filter, priority filter
- 3:00 Task outputs: JSON result file for parsing by downstream tasks
- 5:00 Running from the command line: ./gradlew runChallenges --platform desktop
- 7:00 Interpreting the output: pass/fail/skip counts and failure details
- 9:00 Failing the build on challenge failures: exit code propagation from Go to Gradle
- 10:00 Selective runs: running only critical-priority challenges in fast feedback loops
- 11:30 Summary

### Code References
- `shared/build.gradle.kts` -- runChallenges task definition
- `Challenges/cmd/` -- Go CLI entry point

---

## Video 35.6: ChallengeValidationTests in the Kotlin Test Suite (12 min)

### Timestamps
- 0:00 Why Kotlin tests for a Go framework: validating the integration layer
- 2:00 ChallengeValidationTests.kt: structure and purpose
- 4:00 Test 1: verify challenge bank files exist and are valid YAML/JSON
- 6:00 Test 2: verify runChallenges Gradle task is registered and executable
- 8:00 Test 3: verify challenge result parsing produces correct pass/fail counts
- 10:00 Keeping validation tests fast: mocking the Go binary in unit tests
- 11:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/` -- ChallengeValidationTests.kt

---

## Video 35.7: HelixQA Test Bank Integration (14 min)

### Timestamps
- 0:00 The relationship: Challenges banks drive automated UI sessions via HelixQA testbank
- 2:00 HelixQA testbank package: YAML management, platform targeting, priority levels
- 4:00 Mapping a Challenge scenario to a HelixQA test bank entry
- 6:00 The evidence pipeline: screenshots, video, logcat captured for each failing scenario
- 8:00 Ticket generation: HelixQA ticket package creating Markdown bug reports from evidence
- 10:00 Closing the loop: ticket -> fix -> challenge passes -> HelixQA session verifies fix
- 12:00 Running the full pipeline: `make qa-all`
- 13:30 Summary

### Code References
- `HelixQA/pkg/testbank/` -- Test bank management package
- `HelixQA/pkg/ticket/` -- Ticket generation package
- `HelixQA/pkg/evidence/` -- Evidence collection package

---

## Video 35.8: Growing the Challenge Framework Over Time (12 min)

### Timestamps
- 0:00 When to add a new bank: every new feature area should have at least one challenge bank
- 2:00 When to add a new scenario: every bug report should become a challenge scenario
- 4:00 Retiring scenarios: when a scenario is superseded by a more comprehensive one
- 6:00 Bank maintenance: updating expected outcomes when intentional behaviour changes
- 8:00 Metrics: tracking challenge pass rate over time as a project health indicator
- 10:00 Open source: making challenge banks available for community contribution
- 11:30 Summary

### Exercises
1. **Schema study**: Read all three existing challenge bank YAML files. List all unique fields used across them and their data types.
2. **New scenario**: Write one new scenario for the protocol-resilience bank covering WebDAV server returning 503. Include steps, expected outcome, and documentation ref.
3. **New bank**: Create a new challenge bank YAML for UI accessibility scenarios. Include at least 3 scenarios targeting the desktop platform.
4. **Gradle task**: Run `./gradlew runChallenges` and interpret the output. Document which scenarios passed, failed, and were skipped.
5. **HelixQA mapping**: Map one scenario from the security challenge bank to a HelixQA test bank entry. Specify the platform, priority, and evidence types to collect.
