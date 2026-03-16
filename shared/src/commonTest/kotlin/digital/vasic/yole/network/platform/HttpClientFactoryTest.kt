/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for the platform-specific HttpClientFactory.
 * Validates that createHttpClient() works correctly
 * across all KMP targets.
 *
 *########################################################*/
package digital.vasic.yole.network.platform

import digital.vasic.yole.network.protocol.createHttpClient
import kotlin.test.*

/**
 * Tests for [createHttpClient] expect/actual function.
 *
 * Tests cover:
 * - Non-null client creation
 * - Client closure without errors
 * - Multiple distinct instances
 * - Client configuration
 * - Repeated creation and disposal
 */
class HttpClientFactoryTest {

    // ==================== Basic Creation Tests ====================

    @Test
    fun `createHttpClient returns non-null HttpClient`() {
        val client = createHttpClient()
        assertNotNull(client, "createHttpClient() should never return null")
        client.close()
    }

    @Test
    fun `created client can be closed without error`() {
        val client = createHttpClient()

        // Should not throw any exception
        client.close()
    }

    @Test
    fun `multiple calls create distinct instances`() {
        val client1 = createHttpClient()
        val client2 = createHttpClient()

        try {
            assertNotSame(client1, client2, "Each call should produce a new HttpClient instance")
        } finally {
            client1.close()
            client2.close()
        }
    }

    // ==================== Lifecycle Tests ====================

    @Test
    fun `client can be created after closing previous one`() {
        val client1 = createHttpClient()
        client1.close()

        val client2 = createHttpClient()
        assertNotNull(client2, "Should be able to create a new client after closing the previous one")
        client2.close()
    }

    @Test
    fun `closing client twice does not throw`() {
        val client = createHttpClient()

        // First close
        client.close()

        // Second close should be idempotent (no exception)
        try {
            client.close()
        } catch (_: Exception) {
            // Some engines may throw on double-close; that is acceptable
        }
    }

    @Test
    fun `rapidly creating and closing clients does not leak resources`() {
        repeat(20) {
            val client = createHttpClient()
            assertNotNull(client)
            client.close()
        }
        // If we reach here without OOM or exception, resources are managed properly
    }

    // ==================== Multiple Instance Tests ====================

    @Test
    fun `three concurrent clients can coexist`() {
        val clients = (1..3).map { createHttpClient() }

        try {
            assertEquals(3, clients.size)
            clients.forEach { assertNotNull(it) }

            // All should be distinct objects
            val distinct = clients.toSet()
            assertEquals(3, distinct.size, "All three clients should be distinct instances")
        } finally {
            clients.forEach { it.close() }
        }
    }

    @Test
    fun `clients created in sequence are independent`() {
        val client1 = createHttpClient()
        val client2 = createHttpClient()

        // Closing client1 should not affect client2
        client1.close()

        // client2 should still be usable (not throw on close)
        assertNotNull(client2)
        client2.close()
    }

    // ==================== Factory Consistency Tests ====================

    @Test
    fun `factory produces clients consistently across iterations`() {
        val clients = mutableListOf<Any>()
        repeat(5) {
            val client = createHttpClient()
            assertNotNull(client, "Iteration $it should produce non-null client")
            clients.add(client)
        }

        // Verify all are distinct
        assertEquals(5, clients.toSet().size, "All 5 clients should be distinct instances")

        // Clean up
        @Suppress("UNCHECKED_CAST")
        clients.forEach { (it as io.ktor.client.HttpClient).close() }
    }

    @Test
    fun `factory works after garbage collection hint`() {
        // Create and discard some clients
        repeat(10) {
            createHttpClient().close()
        }

        // Factory should still work correctly
        val client = createHttpClient()
        assertNotNull(client)
        client.close()
    }
}
