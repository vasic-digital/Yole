<!--
SPDX-FileCopyrightText: 2024-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Security Event Logging

This document describes the `SecurityEventLogger` system, which provides a structured audit trail for security-relevant events across all protocol services.

---

## Purpose

The `SecurityEventLogger` is a singleton object that captures and retains security events in memory. It serves three roles:

1. **Audit trail** -- every authentication failure, path traversal attempt, circuit breaker trip, and token lifecycle event is recorded with structured metadata.
2. **Real-time alerting** -- an optional `listener` callback receives events as they are logged, enabling external forwarding to monitoring systems.
3. **Diagnostic querying** -- events can be filtered by type and severity at runtime, which is useful for debugging connection problems or investigating suspicious activity.

**Source:** `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/SecurityEventLogger.kt`

---

## Event Types

The `SecurityEventType` enum defines 10 categories of security events:

| Event Type | Description | Default Severity |
|------------|-------------|-----------------|
| `AUTH_FAILURE` | Failed authentication attempt (bad credentials, expired token) | ERROR |
| `AUTH_SUCCESS` | Successful authentication | INFO |
| `TOKEN_REFRESH` | Token refresh operation succeeded | INFO |
| `TOKEN_EXPIRED` | Token expiration detected or refresh failed | WARNING |
| `PATH_TRAVERSAL_BLOCKED` | Path traversal attempt blocked by `normalizePath()` | ERROR |
| `CONNECTION_REFUSED` | Connection attempt refused by remote server | WARNING |
| `CIRCUIT_BREAKER_OPEN` | Circuit breaker opened due to accumulated failures | WARNING |
| `SECURE_STORAGE_ERROR` | SecureStorage operation failure (read/write/delete) | CRITICAL |
| `INVALID_CERTIFICATE` | TLS certificate validation failure | CRITICAL |
| `RATE_LIMITED` | Rate limit threshold reached, operation throttled | WARNING |

---

## Severity Levels

The `SecurityEventSeverity` enum defines 4 levels, ordered from least to most severe:

| Severity | Meaning | Examples |
|----------|---------|----------|
| `INFO` | Normal operations, no action required | Successful auth, token refresh |
| `WARNING` | Potentially concerning, may need attention | Rate limited, circuit breaker open, token expired |
| `ERROR` | Security violations that were blocked | Auth failure, path traversal blocked |
| `CRITICAL` | Severe security events requiring investigation | Invalid certificate, secure storage corruption |

---

## SecurityEvent Data Class

Each event is an immutable `data class` with the following fields:

```kotlin
data class SecurityEvent(
    val type: SecurityEventType,          // Category of security event
    val severity: SecurityEventSeverity,  // Severity level
    val service: String,                  // Protocol service name (e.g., "DropboxService")
    val message: String,                  // Human-readable description
    val details: Map<String, String>,     // Structured metadata (default: emptyMap())
    val timestamp: Long                   // Epoch milliseconds (default: Clock.System.now())
)
```

**Important:** The `details` map must never include secrets (passwords, tokens, API keys). Only safe-to-log metadata such as attempted paths, failure counts, operation names, and user-visible identifiers should be included.

---

## Usage Examples

### Logging Events via Convenience Methods

```kotlin
// Authentication failure with optional details
SecurityEventLogger.logAuthFailure(
    "DropboxService",
    "Invalid refresh token",
    mapOf("userId" to "user-123", "attempt" to "3")
)

// Path traversal attempt blocked
SecurityEventLogger.logPathTraversalBlocked("FtpService", "../../etc/passwd")

// Circuit breaker opened
SecurityEventLogger.logCircuitBreakerOpen("SftpService", failureCount = 5)

// Token refresh (success or failure)
SecurityEventLogger.logTokenRefresh("GoogleDriveService", success = true)
SecurityEventLogger.logTokenRefresh("OneDriveService", success = false)

// Rate limited operation
SecurityEventLogger.logRateLimited("SmbService", "listFiles")
```

### Logging Events Directly

For event types without convenience methods (e.g., `AUTH_SUCCESS`, `CONNECTION_REFUSED`, `SECURE_STORAGE_ERROR`, `INVALID_CERTIFICATE`), use the `log()` method directly:

```kotlin
SecurityEventLogger.log(
    SecurityEvent(
        type = SecurityEventType.INVALID_CERTIFICATE,
        severity = SecurityEventSeverity.CRITICAL,
        service = "WebDavService",
        message = "TLS certificate validation failed",
        details = mapOf("host" to "cloud.example.com", "error" to "self-signed")
    )
)
```

---

## Configuration

### maxEvents

Controls how many events are retained in memory. When the limit is exceeded, the oldest events are evicted first (ring buffer behavior).

```kotlin
SecurityEventLogger.maxEvents = 5000  // Default: 1000
```

### listener

An optional callback invoked for every logged event. The listener is called outside the internal lock, so it is safe to perform slow operations (e.g., network calls) in the callback without blocking the logger.

```kotlin
SecurityEventLogger.listener = { event ->
    println("[${event.severity}] ${event.service}: ${event.message}")
}
```

Set to `null` to disable:

```kotlin
SecurityEventLogger.listener = null
```

---

## Query API

### getEvents

Returns a snapshot list of events, optionally filtered by type and/or severity:

```kotlin
// All events
val all = SecurityEventLogger.getEvents()

// Only authentication failures
val authFailures = SecurityEventLogger.getEvents(type = SecurityEventType.AUTH_FAILURE)

// Only errors
val errors = SecurityEventLogger.getEvents(severity = SecurityEventSeverity.ERROR)

// Specific type AND severity
val criticalCertErrors = SecurityEventLogger.getEvents(
    type = SecurityEventType.INVALID_CERTIFICATE,
    severity = SecurityEventSeverity.CRITICAL
)
```

### eventCount

Returns the number of events currently retained:

```kotlin
val count = SecurityEventLogger.eventCount()
```

### clear

Removes all retained events:

```kotlin
SecurityEventLogger.clear()
```

---

## Thread-Safety

All mutable state in `SecurityEventLogger` is protected by `platformSynchronized(lock)`, which delegates to the `Concurrency-KMP` module's platform-appropriate synchronization primitive:

- **JVM/Android:** `synchronized(lock) { ... }`
- **Native (iOS):** `kotlin.native.concurrent.withLock { ... }`
- **Wasm:** Single-threaded, no contention

This approach is appropriate because all internal operations are fast in-memory list manipulations with no I/O or suspension points. The `listener` callback is invoked outside the lock to avoid holding it during potentially slow external processing.

The concurrent safety is validated by `SecurityEventLoggerTests.concurrentLoggingFromFiftyCoroutines()`, which logs from 50 concurrent coroutines and verifies all events are recorded.

---

## Integration Points

The following table shows which protocol services should call which log methods. Each of the 8 protocol services (Dropbox, FTP, Git, Google Drive, OneDrive, SFTP, SMB, WebDAV) should integrate these calls at the appropriate points:

| Call Site | Log Method | When |
|-----------|-----------|------|
| Authentication flow | `logAuthFailure()` | Credentials rejected, token invalid |
| Authentication flow | `log(AUTH_SUCCESS)` | Authentication succeeds |
| Token manager | `logTokenRefresh()` | After refresh attempt (pass success/failure) |
| `normalizePath()` in PathUtils | `logPathTraversalBlocked()` | Path contains `..` or escapes root |
| CircuitBreaker state transition | `logCircuitBreakerOpen()` | Circuit transitions from CLOSED to OPEN |
| ConnectionLimiter / RateLimiter | `logRateLimited()` | Operation rejected due to rate/connection limit |
| SecureStorage read/write | `log(SECURE_STORAGE_ERROR)` | Keychain/KeyStore operation fails |
| TLS/HTTP client setup | `log(INVALID_CERTIFICATE)` | Certificate validation fails |
| Connection establishment | `log(CONNECTION_REFUSED)` | TCP connection refused |

---

## Platform Output Reporters

Currently, events are retained in memory only. Future work includes platform-specific reporters that forward events to native logging systems:

| Platform | Reporter | Output |
|----------|----------|--------|
| Android | `LogcatSecurityReporter` | `android.util.Log` with `SECURITY` tag |
| Desktop (JVM) | `StderrSecurityReporter` | `System.err` with timestamp and structured format |
| iOS | `OSLogSecurityReporter` | `os_log` with `.fault` level for CRITICAL events |
| Web (Wasm) | `ConsoleSecurityReporter` | `console.warn()` / `console.error()` based on severity |

These reporters would be registered as listeners:

```kotlin
// Example future usage on Android
SecurityEventLogger.listener = LogcatSecurityReporter()::onEvent
```

---

## Testing

Tests are located at `shared/src/commonTest/kotlin/digital/vasic/yole/security/SecurityEventLoggerTests.kt` and cover 16 test cases:

- Convenience method logging (auth failure, path traversal, circuit breaker, token refresh success/failure, rate limited)
- Filtering by type, severity, and both
- Event count and clear
- Max events eviction policy (oldest-first)
- Listener callback delivery
- Timestamp population
- Details map preservation
- Concurrent logging from 50 coroutines
- SecurityEvent data class equality and copy

Run with:

```bash
./gradlew :shared:desktopTest --tests "digital.vasic.yole.security.SecurityEventLoggerTests"
```

---

## Related Documentation

- [Security Scanning Guide](SECURITY_SCANNING.md) -- Static analysis and vulnerability scanning
- [Resilience Patterns](RESILIENCE_PATTERNS.md) -- CircuitBreaker and ConnectionLimiter details
- [Concurrency Safety](CONCURRENCY_SAFETY.md) -- Thread-safety patterns used across Yole
- [Lock Ordering](LOCK_ORDERING.md) -- Mutex priority levels in protocol services

---

*Last updated: March 19, 2026*
