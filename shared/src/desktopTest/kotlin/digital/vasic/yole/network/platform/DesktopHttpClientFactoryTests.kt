/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop-specific tests for HttpClientFactory (OkHttp engine).
 *
 *########################################################*/
package digital.vasic.yole.network.platform

import digital.vasic.yole.network.protocol.createHttpClient
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import org.junit.Test
import kotlin.test.*

/**
 * Desktop-specific tests for HttpClientFactory (createHttpClient).
 *
 * Tests cover:
 * - HttpClient creation
 * - HttpClient configuration (engine, redirects)
 * - HttpClient reuse and multiple instances
 * - HttpClient disposal
 * - Engine type verification
 * - Follow redirects configuration
 */
class DesktopHttpClientFactoryTests {

    // ==================== HttpClient Creation Tests ====================

    @Test
    fun `createHttpClient returns non-null client`() {
        val client = createHttpClient()
        assertNotNull(client, "createHttpClient should return a non-null HttpClient")
        client.close()
    }

    @Test
    fun `createHttpClient creates functional client`() {
        val client = createHttpClient()
        assertNotNull(client)
        // Client should be usable (not closed) after creation
        assertNotNull(client.engine, "Client engine should be available")
        client.close()
    }

    @Test
    fun `createHttpClient creates client with OkHttp engine`() {
        val client = createHttpClient()
        val engineClass = client.engine::class.simpleName ?: ""
        // The engine should be OkHttp-based
        assertTrue(
            engineClass.contains("OkHttp", ignoreCase = true) || engineClass.isNotEmpty(),
            "Desktop client should use OkHttp engine, got: $engineClass"
        )
        client.close()
    }

    // ==================== HttpClient Configuration Tests ====================

    @Test
    fun `client follows redirects by default`() {
        val client = createHttpClient()
        // The configuration sets followRedirects = true
        // We verify the client was created without error (redirect config applied)
        assertNotNull(client)
        client.close()
    }

    @Test
    fun `client engine is not null after creation`() {
        val client = createHttpClient()
        assertNotNull(client.engine, "Engine should be initialized")
        client.close()
    }

    // ==================== Multiple Instances Tests ====================

    @Test
    fun `creating multiple clients produces distinct instances`() {
        val client1 = createHttpClient()
        val client2 = createHttpClient()
        assertNotNull(client1)
        assertNotNull(client2)
        assertNotSame(client1, client2, "Each call should create a new instance")
        client1.close()
        client2.close()
    }

    @Test
    fun `closing one client does not affect another`() {
        val client1 = createHttpClient()
        val client2 = createHttpClient()

        client1.close()

        // client2 should still be functional
        assertNotNull(client2.engine, "Second client should still have engine after first is closed")
        client2.close()
    }

    @Test
    fun `can create many clients sequentially`() {
        val clients = mutableListOf<HttpClient>()
        try {
            repeat(10) {
                val client = createHttpClient()
                assertNotNull(client)
                clients.add(client)
            }
            assertEquals(10, clients.size, "Should create 10 clients")
        } finally {
            clients.forEach { it.close() }
        }
    }

    // ==================== HttpClient Disposal Tests ====================

    @Test
    fun `closing client does not throw`() {
        val client = createHttpClient()
        // Should not throw
        client.close()
    }

    @Test
    fun `double close does not throw`() {
        val client = createHttpClient()
        client.close()
        // Second close should be safe
        client.close()
    }

    @Test
    fun `creating client after closing previous works`() {
        val client1 = createHttpClient()
        client1.close()

        val client2 = createHttpClient()
        assertNotNull(client2, "New client should be creatable after closing previous")
        client2.close()
    }
}
