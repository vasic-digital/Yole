# ADR 002: Facade Bridge Pattern for Migration

<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

## Status

Accepted (2026-03-06)

## Context

After extracting 10 KMP modules (ADR-001), existing Yole code imports types from `digital.vasic.yole.*` packages. Renaming all imports to new module packages (`digital.vasic.ratelimiter.*`, etc.) would require touching hundreds of files simultaneously, creating a risky big-bang migration.

## Decision

Create thin facade bridge files that use Kotlin `typealias` to re-export types from extracted modules under the original `digital.vasic.yole.*` package:

- `util/RateLimiting.kt` — RateLimiter, TokenBucket, AdaptiveRateLimiter, OperationThrottler
- `util/LazyLoading.kt` — LazyDocumentLoader, LazyStringLoader, FlowLazyLoader
- `util/PlatformSync.kt` — platformSynchronized() delegate
- `network/auth/OAuth2Flow.kt` — OAuth2Flow, TokenResponse, provider-specific flows

Types that cannot use typealiases (due to Kotlin limitations with nested objects, sealed class pattern matching, and expect/actual declarations) remain as original code in Yole:
- `ui/Theme.kt`, `ui/Animations.kt`, `ui/Accessibility.kt`
- `network/common/StorageConfig.kt`, `network/auth/AuthTokenManager.kt`
- All `network/platform/` expect/actual files

## Consequences

**Positive:**
- Zero-risk incremental migration (no import changes needed)
- Existing tests continue to compile without modification
- Can remove facades one-at-a-time as code migrates to new imports

**Negative:**
- Facade files add indirection (minor IDE navigation overhead)
- Must maintain awareness of which types are facaded vs original
- Kotlin typealias limitations prevent facading all types
