/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for SecurityEventLogger structured audit trail.
 * Validates logging, filtering, eviction, listener, and
 * concurrent access patterns across all KMP targets.
 *
 *########################################################*/
package digital.vasic.yole.security

import digital.vasic.yole.network.common.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Tests for [SecurityEventLogger].
 *
 * Covers:
 * - Logging via convenience methods (auth failure, path traversal, circuit breaker, token, rate limit)
 * - Filtering by type, severity, and both
 * - Event count and clear
 * - Max events eviction policy
 * - Listener callback
 * - Timestamp population
 * - Details map preservation
 * - Concurrent logging safety
 * - SecurityEvent data class equality and copy
 *
 * Total: 16 tests
 */
class SecurityEventLoggerTests {

    @BeforeTest
    fun setUp() {
        SecurityEventLogger.clear()
        SecurityEventLogger.listener = null
        SecurityEventLogger.maxEvents = 1000
    }

    @AfterTest
    fun tearDown() {
        SecurityEventLogger.clear()
        SecurityEventLogger.listener = null
        SecurityEventLogger.maxEvents = 1000
    }

    // ==================== Convenience Method Tests ====================

    @Test
    fun logAuthFailureRecordsEventWithCorrectTypeAndSeverity() {
        SecurityEventLogger.logAuthFailure("DropboxService", "Invalid refresh token")

        val events = SecurityEventLogger.getEvents()
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals(SecurityEventType.AUTH_FAILURE, event.type)
        assertEquals(SecurityEventSeverity.ERROR, event.severity)
        assertEquals("DropboxService", event.service)
        assertEquals("Invalid refresh token", event.message)
    }

    @Test
    fun logPathTraversalBlockedRecordsEventWithAttemptedPath() {
        SecurityEventLogger.logPathTraversalBlocked("FtpService", "../../etc/passwd")

        val events = SecurityEventLogger.getEvents()
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals(SecurityEventType.PATH_TRAVERSAL_BLOCKED, event.type)
        assertEquals(SecurityEventSeverity.ERROR, event.severity)
        assertEquals("FtpService", event.service)
        assertEquals("Path traversal attempt blocked", event.message)
        assertEquals("../../etc/passwd", event.details["attemptedPath"])
    }

    @Test
    fun logCircuitBreakerOpenRecordsEventWithFailureCount() {
        SecurityEventLogger.logCircuitBreakerOpen("SftpService", 5)

        val events = SecurityEventLogger.getEvents()
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals(SecurityEventType.CIRCUIT_BREAKER_OPEN, event.type)
        assertEquals(SecurityEventSeverity.WARNING, event.severity)
        assertEquals("SftpService", event.service)
        assertEquals("Circuit breaker opened after 5 failures", event.message)
        assertEquals("5", event.details["failureCount"])
    }

    @Test
    fun logTokenRefreshSuccessRecordsCorrectTypeAndSeverity() {
        SecurityEventLogger.logTokenRefresh("GoogleDriveService", success = true)

        val events = SecurityEventLogger.getEvents()
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals(SecurityEventType.TOKEN_REFRESH, event.type)
        assertEquals(SecurityEventSeverity.INFO, event.severity)
        assertEquals("GoogleDriveService", event.service)
        assertEquals("Token refreshed", event.message)
    }

    @Test
    fun logTokenRefreshFailureRecordsExpiredTypeAndWarningSeverity() {
        SecurityEventLogger.logTokenRefresh("OneDriveService", success = false)

        val events = SecurityEventLogger.getEvents()
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals(SecurityEventType.TOKEN_EXPIRED, event.type)
        assertEquals(SecurityEventSeverity.WARNING, event.severity)
        assertEquals("OneDriveService", event.service)
        assertEquals("Token refresh failed", event.message)
    }

    @Test
    fun logRateLimitedRecordsEventWithOperationDetail() {
        SecurityEventLogger.logRateLimited("SmbService", "listFiles")

        val events = SecurityEventLogger.getEvents()
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals(SecurityEventType.RATE_LIMITED, event.type)
        assertEquals(SecurityEventSeverity.WARNING, event.severity)
        assertEquals("SmbService", event.service)
        assertEquals("Rate limited: listFiles", event.message)
        assertEquals("listFiles", event.details["operation"])
    }

    // ==================== Filter Tests ====================

    @Test
    fun getEventsFiltersByType() {
        SecurityEventLogger.logAuthFailure("Svc1", "fail1")
        SecurityEventLogger.logPathTraversalBlocked("Svc2", "/bad/path")
        SecurityEventLogger.logAuthFailure("Svc3", "fail2")
        SecurityEventLogger.logCircuitBreakerOpen("Svc4", 3)

        val authFailures = SecurityEventLogger.getEvents(type = SecurityEventType.AUTH_FAILURE)
        assertEquals(2, authFailures.size)
        assertTrue(authFailures.all { it.type == SecurityEventType.AUTH_FAILURE })
    }

    @Test
    fun getEventsFiltersBySeverity() {
        SecurityEventLogger.logAuthFailure("Svc1", "fail1") // ERROR
        SecurityEventLogger.logCircuitBreakerOpen("Svc2", 3) // WARNING
        SecurityEventLogger.logTokenRefresh("Svc3", success = true) // INFO
        SecurityEventLogger.logRateLimited("Svc4", "op1") // WARNING
        SecurityEventLogger.logPathTraversalBlocked("Svc5", "/x") // ERROR

        val warnings = SecurityEventLogger.getEvents(severity = SecurityEventSeverity.WARNING)
        assertEquals(2, warnings.size)
        assertTrue(warnings.all { it.severity == SecurityEventSeverity.WARNING })

        val errors = SecurityEventLogger.getEvents(severity = SecurityEventSeverity.ERROR)
        assertEquals(2, errors.size)
        assertTrue(errors.all { it.severity == SecurityEventSeverity.ERROR })
    }

    @Test
    fun getEventsFiltersByBothTypeAndSeverity() {
        SecurityEventLogger.logAuthFailure("Svc1", "fail1") // AUTH_FAILURE + ERROR
        SecurityEventLogger.logCircuitBreakerOpen("Svc2", 3) // CIRCUIT_BREAKER_OPEN + WARNING
        SecurityEventLogger.logPathTraversalBlocked("Svc3", "/x") // PATH_TRAVERSAL_BLOCKED + ERROR
        SecurityEventLogger.logRateLimited("Svc4", "op1") // RATE_LIMITED + WARNING

        val result = SecurityEventLogger.getEvents(
            type = SecurityEventType.AUTH_FAILURE,
            severity = SecurityEventSeverity.ERROR
        )
        assertEquals(1, result.size)
        assertEquals("Svc1", result.first().service)
    }

    // ==================== Count and Clear Tests ====================

    @Test
    fun eventCountReturnsCorrectCount() {
        assertEquals(0, SecurityEventLogger.eventCount())

        SecurityEventLogger.logAuthFailure("Svc1", "fail1")
        assertEquals(1, SecurityEventLogger.eventCount())

        SecurityEventLogger.logPathTraversalBlocked("Svc2", "/bad")
        assertEquals(2, SecurityEventLogger.eventCount())

        SecurityEventLogger.logCircuitBreakerOpen("Svc3", 5)
        assertEquals(3, SecurityEventLogger.eventCount())
    }

    @Test
    fun clearRemovesAllEvents() {
        SecurityEventLogger.logAuthFailure("Svc1", "fail1")
        SecurityEventLogger.logPathTraversalBlocked("Svc2", "/bad")
        SecurityEventLogger.logCircuitBreakerOpen("Svc3", 5)
        assertEquals(3, SecurityEventLogger.eventCount())

        SecurityEventLogger.clear()

        assertEquals(0, SecurityEventLogger.eventCount())
        assertTrue(SecurityEventLogger.getEvents().isEmpty())
    }

    // ==================== Eviction Test ====================

    @Test
    fun maxEventsEvictsOldestWhenExceeded() {
        SecurityEventLogger.maxEvents = 5

        repeat(10) { i ->
            SecurityEventLogger.logAuthFailure("Svc$i", "fail-$i")
        }

        assertEquals(5, SecurityEventLogger.eventCount())

        // The oldest 5 events (Svc0..Svc4) should have been evicted
        val events = SecurityEventLogger.getEvents()
        assertEquals(5, events.size)
        assertEquals("Svc5", events[0].service)
        assertEquals("Svc6", events[1].service)
        assertEquals("Svc7", events[2].service)
        assertEquals("Svc8", events[3].service)
        assertEquals("Svc9", events[4].service)
    }

    // ==================== Listener Test ====================

    @Test
    fun listenerReceivesEventsInRealTime() {
        val received = mutableListOf<SecurityEvent>()
        SecurityEventLogger.listener = { event -> received.add(event) }

        SecurityEventLogger.logAuthFailure("Svc1", "fail1")
        SecurityEventLogger.logPathTraversalBlocked("Svc2", "/bad")

        assertEquals(2, received.size)
        assertEquals(SecurityEventType.AUTH_FAILURE, received[0].type)
        assertEquals(SecurityEventType.PATH_TRAVERSAL_BLOCKED, received[1].type)
    }

    // ==================== Timestamp and Details Tests ====================

    @Test
    fun eventTimestampIsPopulated() {
        val beforeMs = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        SecurityEventLogger.logAuthFailure("Svc1", "fail1")
        val afterMs = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

        val event = SecurityEventLogger.getEvents().first()
        assertTrue(
            event.timestamp in beforeMs..afterMs,
            "Timestamp ${event.timestamp} should be between $beforeMs and $afterMs"
        )
    }

    @Test
    fun eventDetailsMapIsPreserved() {
        val details = mapOf("userId" to "user-123", "ipAddress" to "192.168.1.1", "attempt" to "3")
        SecurityEventLogger.logAuthFailure("Svc1", "fail1", details)

        val event = SecurityEventLogger.getEvents().first()
        assertEquals(3, event.details.size)
        assertEquals("user-123", event.details["userId"])
        assertEquals("192.168.1.1", event.details["ipAddress"])
        assertEquals("3", event.details["attempt"])
    }

    // ==================== Concurrent Logging Test ====================

    @Test
    fun concurrentLoggingFromFiftyCoroutines() = runBlocking<Unit> {
        val jobs = (1..50).map { i ->
            async {
                SecurityEventLogger.logAuthFailure("ConcurrentSvc$i", "fail-$i")
            }
        }
        jobs.awaitAll()

        assertEquals(50, SecurityEventLogger.eventCount())

        val events = SecurityEventLogger.getEvents()
        assertEquals(50, events.size)

        // All 50 unique services should be present
        val services = events.map { it.service }.toSet()
        assertEquals(50, services.size)
    }

    // ==================== Data Class Tests ====================

    @Test
    fun securityEventDataClassEqualityAndCopy() {
        val event1 = SecurityEvent(
            type = SecurityEventType.AUTH_FAILURE,
            severity = SecurityEventSeverity.ERROR,
            service = "TestService",
            message = "test message",
            details = mapOf("key" to "value"),
            timestamp = 1000L
        )
        val event2 = SecurityEvent(
            type = SecurityEventType.AUTH_FAILURE,
            severity = SecurityEventSeverity.ERROR,
            service = "TestService",
            message = "test message",
            details = mapOf("key" to "value"),
            timestamp = 1000L
        )

        // Equality
        assertEquals(event1, event2)
        assertEquals(event1.hashCode(), event2.hashCode())

        // Copy with modification
        val modified = event1.copy(severity = SecurityEventSeverity.CRITICAL)
        assertNotEquals(event1, modified)
        assertEquals(SecurityEventSeverity.CRITICAL, modified.severity)
        assertEquals(event1.type, modified.type)
        assertEquals(event1.service, modified.service)
        assertEquals(event1.message, modified.message)
        assertEquals(event1.details, modified.details)
        assertEquals(event1.timestamp, modified.timestamp)
    }
}
