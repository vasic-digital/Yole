# KMP Module Documentation Status

**Date**: 2026-03-17

This document tracks the documentation status of the 10 extracted KMP modules that Yole consumes via `includeBuild()` in `settings.gradle.kts`. These are external repositories in sibling directories (`../ModuleName-KMP`).

## Module Overview

| Module | Package | CHANGELOG.md | CONTRIBUTING.md | README.md | CI/CD |
|--------|---------|:------------:|:---------------:|:---------:|:-----:|
| RateLimiter-KMP | `digital.vasic.ratelimiter` | Done | Done | Done | Done |
| Concurrency-KMP | `digital.vasic.concurrency` | Done | Done | Done | Done |
| UI-Components-KMP | `digital.vasic.uicomponents` | Done | Done | Done | Done |
| Auth-KMP | `digital.vasic.auth` | Done | Done | Done | Done |
| Security-KMP | `digital.vasic.security` | Done | Done | Done | Done |
| Document-KMP | `digital.vasic.document` | Done | Done | Done | Done |
| Config-KMP | `digital.vasic.config` | Done | Done | Done | Done |
| Database-KMP | `digital.vasic.database` | Done | Done | Done | Done |
| Storage-KMP | `digital.vasic.storage` | Done | Done | Done | Done |
| Formatters-KMP | `digital.vasic.formatters` | Done | Done | Done | Done |

## Documentation Tasks Completed

All 10 KMP modules have the following documentation in place (completed in Session 3, Phase 6):

1. **CHANGELOG.md** -- Version history following Keep a Changelog format
2. **CONTRIBUTING.md** -- Contribution guidelines including code style, testing, and PR process
3. **README.md** -- Module overview, installation, API reference, and usage examples
4. **CI/CD** -- GitHub Actions workflows for build, test, and publish

## Recommended Future Documentation

The following documentation improvements are recommended for each module:

### API Reference (KDoc)
- Run `./gradlew :moduleName:dokkaHtml` to generate API docs
- Publish to GitHub Pages for each module
- Add links from Yole's main documentation

### Architecture Decision Records
- Document why each module was extracted from Yole
- Document the public API surface decisions
- Document platform-specific implementation choices

### Migration Guides
- Document how to upgrade between major versions
- Include breaking change notes with migration steps
- Provide code transformation examples

### Integration Guides
- Document how Yole consumes each module via `includeBuild()`
- Document the facade bridge pattern used in Yole (`util/RateLimiting.kt`, `util/LazyLoading.kt`, etc.)
- Document type alias limitations (nested objects, sealed classes, expect/actual)

### Performance Documentation
- Benchmark results for each module's core operations
- Memory profiling data
- Comparison with alternative libraries

## Facade Bridges in Yole

Some Yole source files are thin typealiases that re-export types from extracted modules under the `digital.vasic.yole.*` package:

| Yole File | Source Module | Purpose |
|-----------|--------------|---------|
| `util/RateLimiting.kt` | RateLimiter-KMP | Re-exports RateLimiter, TokenBucket, AdaptiveRateLimiter, OperationThrottler |
| `util/LazyLoading.kt` | Concurrency-KMP | Re-exports LazyDocumentLoader, LazyStringLoader, FlowLazyLoader |
| `util/PlatformSync.kt` | Concurrency-KMP | Re-exports platform synchronization primitives |
| `network/auth/OAuth2Flow.kt` | Auth-KMP | Re-exports OAuth2Flow types |

Types with nested objects, sealed class pattern matching, or expect/actual declarations cannot use typealiases and remain as original code in Yole.
