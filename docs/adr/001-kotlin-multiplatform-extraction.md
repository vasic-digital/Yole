# ADR 001: Kotlin Multiplatform Module Extraction

<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

## Status

Accepted (2026-03-06)

## Context

The Yole shared module contained all business logic in a single monolithic module. As the codebase grew to 67+ source files with 10 distinct concerns (rate limiting, concurrency, auth, security, storage, etc.), the module became difficult to test independently, reuse across projects, and maintain clear boundaries.

## Decision

Extract 10 independent KMP modules, each with its own repository, CI/CD, tests, and documentation:

1. RateLimiter-KMP (`digital.vasic.ratelimiter`)
2. Concurrency-KMP (`digital.vasic.concurrency`)
3. UI-Components-KMP (`digital.vasic.uicomponents`)
4. Auth-KMP (`digital.vasic.auth`)
5. Security-KMP (`digital.vasic.security`)
6. Document-KMP (`digital.vasic.document`)
7. Config-KMP (`digital.vasic.config`)
8. Database-KMP (`digital.vasic.database`)
9. Storage-KMP (`digital.vasic.storage`)
10. Formatters-KMP (`digital.vasic.formatters`)

Modules are consumed via Gradle `includeBuild()` composite builds in `settings.gradle.kts`, living as sibling directories (`../ModuleName-KMP`).

## Consequences

**Positive:**
- Each module independently testable and releasable
- Clear dependency boundaries enforced by module system
- Reusable across other Kotlin Multiplatform projects
- Smaller compilation units = faster incremental builds
- Each module has dedicated CHANGELOG.md and CONTRIBUTING.md

**Negative:**
- Requires facade bridges (see ADR-002) during migration
- 10 additional repositories to maintain
- Composite builds add complexity to `settings.gradle.kts`
- Version synchronization across modules needs discipline
