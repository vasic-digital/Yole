/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Structured security event logging for audit trail
 *
 *########################################################*/
package digital.vasic.yole.network.common

import digital.vasic.yole.util.platformSynchronized
import kotlinx.datetime.Clock

/**
 * Types of security events that can be logged for audit trail purposes.
 */
enum class SecurityEventType {
    /** Failed authentication attempt */
    AUTH_FAILURE,
    /** Successful authentication */
    AUTH_SUCCESS,
    /** Token refresh operation */
    TOKEN_REFRESH,
    /** Token expiration detected */
    TOKEN_EXPIRED,
    /** Path traversal attempt blocked by normalizePath */
    PATH_TRAVERSAL_BLOCKED,
    /** Connection attempt refused */
    CONNECTION_REFUSED,
    /** Circuit breaker opened due to failures */
    CIRCUIT_BREAKER_OPEN,
    /** SecureStorage operation failure */
    SECURE_STORAGE_ERROR,
    /** TLS certificate validation failure */
    INVALID_CERTIFICATE,
    /** Rate limit threshold reached */
    RATE_LIMITED
}

/**
 * Severity levels for security events, ordered from least to most severe.
 */
enum class SecurityEventSeverity {
    /** Normal operations (auth success, token refresh) */
    INFO,
    /** Potentially concerning (rate limited, circuit breaker) */
    WARNING,
    /** Security violations (path traversal, auth failure) */
    ERROR,
    /** Severe security events (invalid cert, storage corruption) */
    CRITICAL
}

/**
 * Immutable record of a security event for audit trail.
 *
 * @param type The category of security event
 * @param severity How severe the event is
 * @param service Protocol service name (e.g., "DropboxService")
 * @param message Human-readable description
 * @param details Structured metadata — must never include secrets (passwords, tokens, keys)
 * @param timestamp Epoch milliseconds when the event occurred
 */
data class SecurityEvent(
    val type: SecurityEventType,
    val severity: SecurityEventSeverity,
    val service: String,
    val message: String,
    val details: Map<String, String> = emptyMap(),
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * Structured security event logger for audit trail.
 *
 * Logs authentication failures, path traversal attempts, token operations,
 * circuit breaker state changes, and connection refusals with structured data.
 *
 * Thread-safety: Uses [platformSynchronized] with an internal lock for all
 * mutable state access. This is appropriate because all operations are fast
 * in-memory list manipulations with no I/O or suspension.
 *
 * Events are retained in memory up to [maxEvents]; oldest events are evicted
 * first when the limit is exceeded. An optional [listener] callback receives
 * events in real-time for external processing (e.g., forwarding to a remote
 * logging service).
 *
 * Usage:
 * ```kotlin
 * SecurityEventLogger.logAuthFailure("DropboxService", "Invalid refresh token")
 * SecurityEventLogger.logPathTraversalBlocked("FtpService", "../../etc/passwd")
 *
 * val errors = SecurityEventLogger.getEvents(severity = SecurityEventSeverity.ERROR)
 * ```
 */
object SecurityEventLogger {

    private val _events = mutableListOf<SecurityEvent>()
    private val lock = Any()

    /** Maximum events to retain in memory (oldest evicted first). */
    var maxEvents: Int = 1000

    /** Optional external listener for real-time event processing. */
    var listener: ((SecurityEvent) -> Unit)? = null

    /**
     * Log a security event.
     *
     * The event is added to the in-memory ring buffer and, if set, the
     * [listener] is invoked outside the lock to avoid holding it during
     * potentially slow external processing.
     *
     * @param event The security event to log
     */
    fun log(event: SecurityEvent) {
        platformSynchronized(lock) {
            _events.add(event)
            if (_events.size > maxEvents) {
                _events.removeAt(0)
            }
        }
        listener?.invoke(event)
    }

    // ==================== Convenience Methods ====================

    /**
     * Log an authentication failure.
     *
     * @param service The protocol service where the failure occurred
     * @param reason Human-readable reason for the failure
     * @param details Additional structured metadata (never include secrets)
     */
    fun logAuthFailure(service: String, reason: String, details: Map<String, String> = emptyMap()) {
        log(SecurityEvent(SecurityEventType.AUTH_FAILURE, SecurityEventSeverity.ERROR, service, reason, details))
    }

    /**
     * Log a blocked path traversal attempt.
     *
     * @param service The protocol service where the attempt was blocked
     * @param attemptedPath The path that was attempted (safe to log)
     */
    fun logPathTraversalBlocked(service: String, attemptedPath: String) {
        log(
            SecurityEvent(
                SecurityEventType.PATH_TRAVERSAL_BLOCKED,
                SecurityEventSeverity.ERROR,
                service,
                "Path traversal attempt blocked",
                mapOf("attemptedPath" to attemptedPath)
            )
        )
    }

    /**
     * Log a circuit breaker opening due to accumulated failures.
     *
     * @param service The protocol service whose circuit breaker opened
     * @param failureCount Number of failures that triggered the opening
     */
    fun logCircuitBreakerOpen(service: String, failureCount: Int) {
        log(
            SecurityEvent(
                SecurityEventType.CIRCUIT_BREAKER_OPEN,
                SecurityEventSeverity.WARNING,
                service,
                "Circuit breaker opened after $failureCount failures",
                mapOf("failureCount" to failureCount.toString())
            )
        )
    }

    /**
     * Log a token refresh operation result.
     *
     * @param service The protocol service performing the token refresh
     * @param success Whether the refresh succeeded
     */
    fun logTokenRefresh(service: String, success: Boolean) {
        val type = if (success) SecurityEventType.TOKEN_REFRESH else SecurityEventType.TOKEN_EXPIRED
        val severity = if (success) SecurityEventSeverity.INFO else SecurityEventSeverity.WARNING
        log(SecurityEvent(type, severity, service, if (success) "Token refreshed" else "Token refresh failed"))
    }

    /**
     * Log a rate-limited operation.
     *
     * @param service The protocol service that was rate limited
     * @param operation The operation that was rate limited
     */
    fun logRateLimited(service: String, operation: String) {
        log(
            SecurityEvent(
                SecurityEventType.RATE_LIMITED,
                SecurityEventSeverity.WARNING,
                service,
                "Rate limited: $operation",
                mapOf("operation" to operation)
            )
        )
    }

    // ==================== Query Methods ====================

    /**
     * Get all events, optionally filtered by type and/or severity.
     *
     * @param type If non-null, only events of this type are returned
     * @param severity If non-null, only events of this severity are returned
     * @return A snapshot list of matching events
     */
    fun getEvents(
        type: SecurityEventType? = null,
        severity: SecurityEventSeverity? = null
    ): List<SecurityEvent> = platformSynchronized(lock) {
        _events.filter { event ->
            (type == null || event.type == type) &&
                (severity == null || event.severity == severity)
        }.toList()
    }

    /**
     * Get the number of events currently retained.
     *
     * @return The event count
     */
    fun eventCount(): Int = platformSynchronized(lock) { _events.size }

    /**
     * Clear all retained events.
     */
    fun clear() = platformSynchronized(lock) { _events.clear() }
}
