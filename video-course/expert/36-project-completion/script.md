<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Module 36: Project Completion & Quality Gates (8 videos)

## Prerequisites

- Modules 9-35 (or equivalent familiarity with the full project)
- Module 31: Project Completion Guide

## Learning Objectives

- Define and enforce zero-defect quality gates for a KMP project
- Apply the Build-Test-Document pipeline to every session of work
- Verify test counts, coverage, and Detekt violations before declaring a phase complete
- Use the 6-phase session structure (audit, implement, test, document, verify, commit) consistently
- Understand how Sessions 1-7 progressively hardened Yole to production quality

---

## Video 36.1: What "Complete" Means for a KMP Project (12 min)

### Timestamps
- 0:00 Introduction: "done" is not the same as "complete" in a production KMP codebase
- 2:00 Dimension 1: correctness -- all tests pass, zero Detekt violations
- 4:00 Dimension 2: safety -- concurrency patterns correct, no resource leaks
- 6:00 Dimension 3: security -- 6-tool scan clean, no hardcoded secrets, SBOM generated
- 8:00 Dimension 4: documentation -- architecture, API docs, user manuals, changelogs current
- 10:00 Dimension 5: observability -- metrics, logging, challenge banks covering all feature areas
- 11:30 Summary: the quality gate checklist

---

## Video 36.2: The Session Structure (14 min)

### Timestamps
- 0:00 Introduction: every work session follows the same 6-phase structure
- 2:00 Phase 1: audit -- identify what is missing, broken, or stale before writing code
- 4:00 Phase 2: implement -- make the smallest correct change that addresses the finding
- 6:00 Phase 3: test -- write tests before considering the fix complete
- 8:00 Phase 4: document -- update CHANGELOG.md, ARCHITECTURE.md, CLAUDE.md, affected guides
- 10:00 Phase 5: verify -- run :shared:desktopTest, Detekt, and challenge banks
- 12:00 Phase 6: commit -- atomic commits with descriptive messages, no --no-verify
- 13:30 Summary

### Code References
- `CLAUDE.md` -- Mandatory rules and build commands
- `Makefile` -- Verification targets: `make test-shared`, `make detekt`, `make challenge`

---

## Video 36.3: Zero-Defect Verification Workflow (14 min)

### Timestamps
- 0:00 The verification sequence: run in this order, fix before proceeding
- 2:00 Step 1: `./gradlew :shared:desktopTest` -- all tests must pass, zero failures
- 4:00 Step 2: `./gradlew detekt` -- zero violations, maxIssues: 0 enforced
- 6:00 Step 3: `./gradlew runChallenges` -- all critical-priority challenges must pass
- 8:00 Step 4: review CHANGELOG.md -- Session entry must be present and accurate
- 10:00 Step 5: review ARCHITECTURE.md -- concurrency and testing sections must be current
- 12:00 Blocking on any failure: do not proceed to commit until all gates are green
- 13:30 Summary

### Code References
- `config/detekt/detekt.yml` -- maxIssues: 0
- `shared/build.gradle.kts` -- Test and Detekt task configuration

---

## Video 36.4: Coverage Gates with Kover (12 min)

### Timestamps
- 0:00 Kover 0.8.3: the coverage tool for Kotlin Multiplatform
- 2:00 Running coverage: `./gradlew test koverHtmlReport`
- 4:00 Interpreting the HTML report: line coverage, branch coverage, per-module breakdown
- 6:00 The 70% minimum gate: enforced in build.gradle.kts via koverVerify
- 8:00 Identifying under-covered modules: protocol services, platform-specific code
- 10:00 Writing tests to close coverage gaps: targeting uncovered branches not uncovered lines
- 11:30 Summary

### Code References
- `shared/build.gradle.kts` -- Kover configuration and koverVerify thresholds

---

## Video 36.5: The Session History — Sessions 1 Through 7 (16 min)

### Timestamps
- 0:00 Overview: 7 sessions from initial KMP extraction to production-hardened codebase
- 2:00 Session 1 (March 6): 10 KMP modules extracted to independent repos on GitHub and GitLab
- 4:00 Session 2 (March 7): safety fixes, security infra, test expansion, resilience utilities
- 5:00 Session 3 (March 8): dead code integration, 10 concurrency fixes, 6,695 tests passing
- 7:00 Session 4 (March 17): comprehensive audit, IDE UI redesign, HelixQA initial integration
- 9:00 Session 5 (March 18): HelixQA expansion (testbank/ticket/evidence), 3 new Go modules
- 11:00 Session 6 (March 19): 9-phase pipeline, 8,347 tests, SecurityEventLogger, PerformanceMetrics
- 13:00 Session 7 (March 26): @Volatile audit, lock ordering fixes, HttpTimeout, 8,928 tests
- 15:00 The trajectory: each session builds on the previous, never breaking what was fixed
- 15:30 Summary

### Code References
- `CHANGELOG.md` -- Full session history
- `docs/superpowers/plans/` -- Session planning documents

---

## Video 36.6: Documentation as a Quality Gate (12 min)

### Timestamps
- 0:00 Why documentation is a gate, not an afterthought
- 2:00 CHANGELOG.md: every session gets an entry before the session ends
- 4:00 ARCHITECTURE.md: concurrency patterns, security tools, and test strategy sections stay current
- 6:00 CLAUDE.md: the authoritative source for build commands, conventions, and known issues
- 8:00 User manuals: version and platform status must match the current release
- 10:00 Video course README: episode count and module listing updated with each session
- 11:30 Summary: the five documents that must be current before a session is closed

---

## Video 36.7: Applying Quality Gates to Your Own KMP Project (14 min)

### Timestamps
- 0:00 Adapting the Yole quality gates to a different KMP project
- 2:00 Scaling down: minimum viable gates for a small team (Detekt + tests + CHANGELOG)
- 4:00 Scaling up: adding SBOM, challenge banks, and autonomous QA for enterprise projects
- 6:00 The mandatory rules pattern: encoding non-negotiable constraints in a CLAUDE.md equivalent
- 8:00 The session structure: applicable regardless of team size or project complexity
- 10:00 The audit-first principle: always audit before implementing
- 12:00 Measuring progress: test count, Detekt violations, challenge pass rate as health metrics
- 13:30 Summary

---

## Video 36.8: From Session 7 Toward Production Release (12 min)

### Timestamps
- 0:00 What remains before a v2.19.0 production release
- 2:00 Container release build: `make container-release` producing all 5 platform artifacts
- 4:00 Release naming convention: Yole-{Platform}-{Version}-{Variant}-{VersionCodeDotted}
- 6:00 Full test suite in container: `make container-test` with all platform targets
- 8:00 Final security scan: 6-tool pipeline on the release branch
- 10:00 Publishing: F-Droid metadata, GitHub release, desktop installers
- 11:30 Summary and course conclusion

### Code References
- `Makefile` -- `make container-release`, `make container-test`
- `releases/` -- Release artifact naming convention
- `CLAUDE.md` -- Release naming convention documentation

### Exercises
1. **Quality gate run**: Execute the full 5-step verification sequence. Document the result of each step and the time taken.
2. **Coverage gap**: Run `./gradlew koverHtmlReport`. Identify the three source files with the lowest line coverage. Write one new test for each.
3. **Session structure practice**: Pick one finding from the most recent Detekt or challenge run. Apply the 6-phase session structure to fix it: audit, implement, test, document, verify, commit.
4. **Documentation audit**: Check that CHANGELOG.md, ARCHITECTURE.md, and the video-course README all agree on the current test count and version. Fix any discrepancies.
5. **Release checklist**: Write a release checklist for Yole v2.19.0 covering all steps from container build to F-Droid metadata update.
