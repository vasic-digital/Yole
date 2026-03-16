# ADR 004: Resilience Patterns

<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

## Status

Accepted (2026-03-07)

## Context

Yole connects to 8 network protocols (Dropbox, Google Drive, OneDrive, WebDAV, FTP, SFTP, SMB, Git). Network operations are inherently unreliable: servers go down, connections drop, rate limits apply. Without resilience patterns, a single failing protocol could cascade into application-wide hangs or resource exhaustion.

## Decision

Implement three resilience patterns integrated into all 8 protocol services:

### CircuitBreaker (`network/common/CircuitBreaker.kt`)
- States: CLOSED (normal) -> OPEN (failing) -> HALF_OPEN (testing recovery)
- Configurable failure threshold and reset timeout
- Protected by Mutex for coroutine safety
- All protocol services check circuit breaker before operations

### ConnectionLimiter (`network/common/ConnectionLimiter.kt`)
- Semaphore-based concurrent connection limiting
- Non-blocking acquire/release via Kotlin coroutines
- Prevents resource exhaustion from too many simultaneous connections
- Configurable per-protocol max concurrent connections

### DocumentCache (`format/DocumentCache.kt`)
- LRU (Least Recently Used) cache for ParsedDocument instances
- Configurable maxSize (default 100)
- Hit/miss tracking with hitRate metric
- Mutex-protected with cooperative yield() for cancellation
- Integrated via FormatRegistry.parseWithCache()

## Consequences

**Positive:**
- Failing protocols auto-isolate (circuit breaker prevents cascade)
- Resource usage bounded (connection limiter prevents exhaustion)
- Repeated parses served from cache (significant performance improvement)
- All patterns thread-safe via Mutex

**Negative:**
- Added complexity in each protocol service
- Circuit breaker state needs monitoring (added monitoring tests)
- Cache invalidation requires explicit management
