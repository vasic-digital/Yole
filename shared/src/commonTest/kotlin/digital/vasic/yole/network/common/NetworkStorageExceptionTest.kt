/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for NetworkStorageException hierarchy
 *
 *########################################################*/
package digital.vasic.yole.network.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NetworkStorageExceptionTest {

    @Test
    fun testConnectionFailed() {
        val ex = NetworkStorageException.ConnectionException.Failed(
            message = "Server unreachable",
            cause = RuntimeException("timeout")
        )
        assertEquals("Server unreachable", ex.message)
        assertEquals("CONNECTION_FAILED", ex.errorCode)
        assertNotNull(ex.cause)
        assertNotNull(ex.timestamp)
    }

    @Test
    fun testConnectionTimeout() {
        val ex = NetworkStorageException.ConnectionException.Timeout(
            timeoutMs = 30000
        )
        assertTrue(ex.message!!.contains("30000"))
        assertEquals("CONNECTION_TIMEOUT", ex.errorCode)
    }

    @Test
    fun testAuthenticationFailed() {
        val ex = NetworkStorageException.ConnectionException.Authentication(
            authType = "OAuth2",
            username = "testuser"
        )
        assertTrue(ex.message!!.contains("testuser"))
        assertTrue(ex.message!!.contains("OAuth2"))
        assertEquals("OAuth2", ex.authType)
        assertEquals("testuser", ex.username)
    }

    @Test
    fun testNotConnected() {
        val ex = NetworkStorageException.ConnectionException.NotConnected()
        assertEquals("Not connected", ex.message)
        assertEquals("NOT_CONNECTED", ex.errorCode)
    }

    @Test
    fun testFromThrowableCreatesException() {
        val cause = RuntimeException("test error")
        val ex = NetworkStorageException.fromThrowable(cause, operation = "download")
        assertNotNull(ex)
        assertTrue(ex is NetworkStorageException.GenericError)
        assertEquals("test error", ex.message)
        assertEquals(cause, ex.cause)
        assertEquals("download", (ex as NetworkStorageException.GenericError).operation)
    }

    @Test
    fun testConnectionFailedDefaultMessage() {
        val ex = NetworkStorageException.ConnectionException.Failed()
        assertEquals("Connection failed", ex.message)
        assertNull(ex.cause)
    }
}
