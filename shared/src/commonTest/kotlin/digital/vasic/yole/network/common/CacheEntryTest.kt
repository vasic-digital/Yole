/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for CacheEntry data class
 *
 *########################################################*/
package digital.vasic.yole.network.common

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class CacheEntryTest {

    private fun createTestEntry(
        isPinned: Boolean = false,
        isInUse: Boolean = false,
        expiresAt: kotlinx.datetime.Instant? = null
    ): CacheEntry {
        val now = Clock.System.now()
        return CacheEntry(
            id = "test-cache-1",
            remoteDocumentId = "doc-1",
            localPath = "/cache/test.txt",
            remotePath = "/remote/test.txt",
            size = 1024L,
            createdAt = now.minus(1.hours),
            lastAccessed = now.minus(30.minutes),
            lastModified = now.minus(1.hours),
            expiresAt = expiresAt,
            isPinned = isPinned,
            isInUse = isInUse,
            accessCount = 5
        )
    }

    @Test
    fun testIsExpiredWhenValid() {
        val entry = createTestEntry()
        assertFalse(entry.isExpired)
    }

    @Test
    fun testIsExpiredWhenExpired() {
        val past = Clock.System.now().minus(1.hours)
        val entry = createTestEntry(expiresAt = past)
        // isValid is computed at construction time, so expired entry has isValid = false
        assertTrue(entry.isExpired)
    }

    @Test
    fun testCanBeEvictedWhenNotPinnedAndNotInUse() {
        val entry = createTestEntry(isPinned = false, isInUse = false)
        assertTrue(entry.canBeEvicted)
    }

    @Test
    fun testCannotBeEvictedWhenPinned() {
        val entry = createTestEntry(isPinned = true)
        assertFalse(entry.canBeEvicted)
    }

    @Test
    fun testCannotBeEvictedWhenInUse() {
        val entry = createTestEntry(isInUse = true)
        assertFalse(entry.canBeEvicted)
    }

    @Test
    fun testCompressionRatioWhenCompressed() {
        val now = Clock.System.now()
        val entry = CacheEntry(
            id = "c1", remoteDocumentId = "d1",
            localPath = "/c", remotePath = "/r",
            size = 500L,
            createdAt = now, lastAccessed = now, lastModified = now,
            compression = "gzip",
            originalSize = 1000L
        )
        assertNotNull(entry.compressionRatio)
        assertEquals(0.5, entry.compressionRatio!!, 0.01)
    }

    @Test
    fun testCompressionRatioNullWhenUncompressed() {
        val entry = createTestEntry()
        assertNull(entry.compressionRatio)
    }

    @Test
    fun testAge() {
        val entry = createTestEntry()
        assertTrue(entry.age > 0, "Age should be positive for entry created in the past")
    }

    @Test
    fun testTimeSinceLastAccess() {
        val entry = createTestEntry()
        assertTrue(entry.timeSinceLastAccess > 0)
    }

    @Test
    fun testAccessFrequency() {
        val entry = createTestEntry()
        assertTrue(entry.accessFrequency > 0.0)
    }

    @Test
    fun testWithAccess() {
        val entry = createTestEntry()
        val accessed = entry.withAccess()
        assertEquals(entry.accessCount + 1, accessed.accessCount)
        assertTrue(accessed.isInUse)
    }

    @Test
    fun testWithNotInUse() {
        val entry = createTestEntry(isInUse = true)
        val released = entry.withNotInUse()
        assertFalse(released.isInUse)
    }

    @Test
    fun testWithExpiration() {
        val entry = createTestEntry()
        val future = Clock.System.now().plus(2.hours)
        val updated = entry.withExpiration(future)
        assertEquals(future, updated.expiresAt)
        assertTrue(updated.isValid)
    }

    @Test
    fun testWithExpirationNull() {
        val entry = createTestEntry()
        val updated = entry.withExpiration(null)
        assertNull(updated.expiresAt)
        assertTrue(updated.isValid)
    }

    @Test
    fun testWithContent() {
        val entry = createTestEntry()
        val now = Clock.System.now()
        val updated = entry.withContent(size = 2048L, checksum = "abc123", lastModified = now)
        assertEquals(2048L, updated.size)
        assertEquals("abc123", updated.checksum)
        assertEquals(now, updated.lastModified)
    }

    @Test
    fun testWithPinStatus() {
        val entry = createTestEntry(isPinned = false)
        val pinned = entry.withPinStatus(true)
        assertTrue(pinned.isPinned)
    }

    @Test
    fun testWithPriority() {
        val entry = createTestEntry()
        val updated = entry.withPriority(200)
        assertEquals(200, updated.priority)
    }

    @Test
    fun testCreate() {
        val entry = CacheEntry.create(
            remoteDocumentId = "doc-new",
            localPath = "/cache/new.txt",
            remotePath = "/remote/new.txt",
            size = 512L,
            contentType = "text/plain"
        )
        assertNotNull(entry.id)
        assertEquals("doc-new", entry.remoteDocumentId)
        assertEquals(512L, entry.size)
        assertEquals("text/plain", entry.contentType)
        assertFalse(entry.isPinned)
    }

    @Test
    fun testCreateWithTtl() {
        val entry = CacheEntry.create(
            remoteDocumentId = "doc-ttl",
            localPath = "/cache/ttl.txt",
            remotePath = "/remote/ttl.txt",
            size = 256L,
            ttl = 3600000L // 1 hour
        )
        assertNotNull(entry.expiresAt)
    }

    @Test
    fun testMock() {
        val entry = CacheEntry.mock()
        assertNotNull(entry.id)
        assertEquals("doc1", entry.remoteDocumentId)
        assertEquals(1024L, entry.size)
        assertEquals(5, entry.accessCount)
    }

    @Test
    fun testDefaultPriority() {
        val entry = createTestEntry()
        assertEquals(100, entry.priority)
    }
}
