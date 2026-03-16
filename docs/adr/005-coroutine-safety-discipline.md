# ADR 005: Coroutine Safety Discipline

<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

## Status

Accepted (2026-03-08)

## Context

Kotlin coroutines provide structured concurrency but require discipline to avoid subtle bugs: leaked scopes, swallowed CancellationExceptions, race conditions on mutable state, and deadlocks from blocking in coroutine context.

During Session 3, 10 critical concurrency issues were identified and fixed. This ADR codifies the patterns that emerged.

## Decision

### Rule 1: CancellationException Must Always Be Rethrown

Every `catch (e: Exception)` block in coroutine code MUST check and rethrow CancellationException:

```kotlin
catch (e: Exception) {
    if (e is kotlin.coroutines.cancellation.CancellationException) throw e
    // handle other exceptions
}
```

This is enforced in all 8 protocol services and DocumentCache.

### Rule 2: Mutable State Protected by Named Mutexes

All mutable state uses dedicated named Mutex instances:
- `stateMutex` — connection state
- `operationsMutex` — active operation tracking
- `cacheMutex` — local cache state
- `syncMutex` — sync status
- `scopeMutex` — coroutine scope lifecycle
- `pauseFlagsMutex` — pause/resume flags

### Rule 3: CoroutineScope Lifecycle Management

- Protocol services use `SupervisorJob() + Dispatchers.Default`
- Scope is cancelled on disconnect and recreated on reconnect
- `initJob` is cancelled before scope recreation
- No GlobalScope usage anywhere in the codebase

### Rule 4: No Blocking I/O in Coroutine Context

- File/network I/O wrapped in `withContext(Dispatchers.IO)`
- No `Thread.sleep()` — use `delay()` instead
- No `Object.wait()` — use `Mutex.withLock`

### Rule 5: Tests Use runBlocking<Unit>

JUnit4 requires `void` return type. `runTest` returns `TestResult`, causing signature mismatch. All coroutine tests use `runBlocking<Unit> { }`.

## Consequences

**Positive:**
- Zero race conditions, deadlocks, or leaked scopes in production
- 1,006 lines of dedicated concurrency tests (ConcurrencyFixesTest.kt)
- Patterns are consistent across all 8 protocol services
- Rules are verifiable by static analysis (Detekt coroutines rules)

**Negative:**
- Verbose catch blocks (CancellationException check is boilerplate)
- Multiple Mutex instances per service add complexity
- runBlocking<Unit> pattern is non-obvious for newcomers
