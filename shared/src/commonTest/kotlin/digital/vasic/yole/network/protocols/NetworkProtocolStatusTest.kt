/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for NetworkProtocolStatus
 *
 *########################################################*/
package digital.vasic.yole.network.protocols

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetworkProtocolStatusTest {

    @Test
    fun testAllProtocolsReturnsEight() {
        val protocols = NetworkProtocolStatus.allProtocols()
        assertEquals(8, protocols.size)
    }

    @Test
    fun testAllProtocolNames() {
        val names = NetworkProtocolStatus.allProtocols().map { it.protocolName }.toSet()
        assertTrue("FTP" in names)
        assertTrue("SFTP" in names)
        assertTrue("SMB/CIFS" in names)
        assertTrue("WebDAV" in names)
        assertTrue("Git" in names)
        assertTrue("Dropbox" in names)
        assertTrue("Google Drive" in names)
        assertTrue("OneDrive" in names)
    }

    @Test
    fun testAllProtocolsFullyImplemented() {
        val protocols = NetworkProtocolStatus.allProtocols()
        assertTrue(protocols.all { it.tier == NetworkProtocolStatus.ImplementationTier.FULLY_IMPLEMENTED })
    }

    @Test
    fun testProtocolsWithRealHttp() {
        val httpProtocols = NetworkProtocolStatus.protocolsWithRealHttp()
        assertEquals(5, httpProtocols.size)
        val names = httpProtocols.map { it.protocolName }.toSet()
        assertTrue("WebDAV" in names)
        assertTrue("Git" in names)
        assertTrue("Dropbox" in names)
        assertTrue("Google Drive" in names)
        assertTrue("OneDrive" in names)
    }

    @Test
    fun testTcpProtocolsDontUseHttpClient() {
        val protocols = NetworkProtocolStatus.allProtocols()
        val tcpProtocols = protocols.filter { !it.usesHttpClient }
        assertEquals(3, tcpProtocols.size)
        val names = tcpProtocols.map { it.protocolName }.toSet()
        assertTrue("FTP" in names)
        assertTrue("SFTP" in names)
        assertTrue("SMB/CIFS" in names)
    }

    @Test
    fun testStubbedProtocolsEmpty() {
        assertTrue(NetworkProtocolStatus.stubbedProtocols().isEmpty())
    }

    @Test
    fun testFullyImplementedProtocols() {
        val fullyImpl = NetworkProtocolStatus.fullyImplementedProtocols()
        assertEquals(8, fullyImpl.size)
    }

    @Test
    fun testProtocolsByTier() {
        val stubbed = NetworkProtocolStatus.protocolsByTier(
            NetworkProtocolStatus.ImplementationTier.STUBBED
        )
        assertTrue(stubbed.isEmpty())

        val full = NetworkProtocolStatus.protocolsByTier(
            NetworkProtocolStatus.ImplementationTier.FULLY_IMPLEMENTED
        )
        assertEquals(8, full.size)
    }

    @Test
    fun testProtocolInfoHasServiceClass() {
        val protocols = NetworkProtocolStatus.allProtocols()
        assertTrue(protocols.all { it.serviceClass.isNotEmpty() })
        assertTrue(protocols.all { it.serviceClass.startsWith("digital.vasic.yole.network.protocols.") })
    }

    @Test
    fun testProtocolInfoHasAuthMechanism() {
        val protocols = NetworkProtocolStatus.allProtocols()
        assertTrue(protocols.all { it.authMechanism.isNotEmpty() })
    }

    @Test
    fun testProtocolInfoHasNotes() {
        val protocols = NetworkProtocolStatus.allProtocols()
        assertTrue(protocols.all { it.notes.isNotEmpty() })
    }
}
